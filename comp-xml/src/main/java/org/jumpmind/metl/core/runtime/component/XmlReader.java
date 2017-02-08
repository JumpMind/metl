/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.properties.TypedProperties;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlReader extends AbstractComponentRuntime {

    public static final String TYPE = "XmlReader";

    public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";

    public final static String SETTING_RELATIVE_PATH = "relative.path";

    public final static String SETTING_READ_TAG = "read.tag";

    public final static String SETTING_READ_TAGS_PER_MESSAGE = "read.tags.per.message";

    public static final String SETTING_MUST_EXIST = "must.exist";

    String runWhen = PER_UNIT_OF_WORK;

    boolean getFileNameFromMessage = false;

    String relativePathAndFile;

    String readTag;

    boolean mustExist;
    
    String encoding = "UTF-8";

    int readTagsPerMessage = 1;

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        getFileNameFromMessage = properties.is(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        relativePathAndFile = properties.get(SETTING_RELATIVE_PATH, relativePathAndFile);
        readTagsPerMessage = properties.getInt(SETTING_READ_TAGS_PER_MESSAGE, readTagsPerMessage);
        mustExist = properties.is(SETTING_MUST_EXIST, mustExist);
        readTag = properties.get(SETTING_READ_TAG, readTag);
        runWhen = properties.get(RUN_WHEN, runWhen);

        if (getComponent().getResource() == null) {
            throw new MisconfiguredException(
                    "A resource has not been selected.  The resource is required if not configured to get the file name from the inbound message");
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        List<String> files = getFilesToRead(inputMessage);

        if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
                || (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
            try {
                processFiles(files, inputMessage, callback, unitOfWorkBoundaryReached);
            } catch (Exception e) {
                throw new IoException(e);
            }
        }
    }

    private List<String> getFilesToRead(Message inputMessage) {
        ArrayList<String> files = null;
        if (getFileNameFromMessage && inputMessage instanceof TextMessage) {
            files = ((TextMessage)inputMessage).getPayload();
        } else {
            files = new ArrayList<String>(1);
            files.add(relativePathAndFile);
        }
        return files;
    }

    void processFiles(List<String> files, Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage)
            throws XmlPullParserException, IOException {
        IDirectory directory = getResourceReference();
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        for (String file : files) {
            ArrayList<String> outboundPayload = new ArrayList<String>();
            if (isNotBlank(file)) {
                log(LogLevel.INFO, "Reading %s", file);
            }
            Map<String, Serializable> headers = new HashMap<>();
            headers.put("source.file.path", file);
            LineNumberReader lineNumberReader = null;
            InputStream parserIs = null;
            try {
                String filePath = resolveParamsAndHeaders(file, inputMessage);
                parserIs = directory.getInputStream(filePath, mustExist);
                if (parserIs != null) {
                    InputStreamReader reader = new InputStreamReader(directory.getInputStream(filePath, mustExist), encoding);
                    parser.setInput(parserIs, encoding);
                    lineNumberReader = new LineNumberReader(reader);
                    lineNumberReader.setLineNumber(1);
                    int startCol = 0;
                    int startLine = 1;
                    int prevEndLine = 1;
                    int prevEndCol = 0;
                    int eventType = parser.getEventType();
                    String line = null;
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                if (StringUtils.isBlank(readTag)) {
                                    readTag = parser.getName();
                                    info("Read tag was not set, defaulting to root tag: " + readTag);
                                }
                                if (parser.getName().equals(readTag)) {
                                    startCol = prevEndCol;
                                    startLine = prevEndLine;
                                }
                                prevEndCol = parser.getColumnNumber();
                                prevEndLine = parser.getLineNumber();
                                break;
                            case XmlPullParser.END_TAG:
                                prevEndCol = parser.getColumnNumber();
                                prevEndLine = parser.getLineNumber();
                                if (parser.getName().equals(readTag)) {
                                    StringBuilder xml = new StringBuilder();
    
                                    forward(startLine, lineNumberReader);
    
                                    int linesToRead = parser.getLineNumber() - lineNumberReader.getLineNumber();
                                    if (lineNumberReader.getLineNumber() > startLine) {
                                        startCol = 0;
                                    }
                                    line = lineNumberReader.readLine();
    
                                    while (linesToRead >= 0 && line != null) {
                                        if (startCol > 0) {
                                            if (line.length() > startCol) {
                                                xml.append(line.substring(startCol)).append("\n");
                                            }
                                            startCol = 0;
                                        } else if (linesToRead == 0) {
                                            if (line.length() > parser.getColumnNumber()) {
                                                xml.append(line.substring(0, parser.getColumnNumber()));
                                            } else {
                                                xml.append(line).append("\n");
                                            }
                                        } else {
                                            xml.append(line).append("\n");
                                        }
    
                                        linesToRead--;
                                        if (linesToRead >= 0) {
                                            line = lineNumberReader.readLine();
                                        }
                                    }
                                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                                    outboundPayload.add(xml.toString());
                                    if (outboundPayload.size() == readTagsPerMessage) {
                                        callback.sendTextMessage(headers, outboundPayload);
                                        outboundPayload = new ArrayList<String>();
                                    }
                                    startCol = 0;
                                }
                                break;
                        }
                        eventType = parser.next();
                    }
                } else {
                    if (isNotBlank(file)) {
                       info("File %s didn't exist, but must exist setting was false.  Continuing",file);
                    }
                }
            } finally {
                closeQuietly(lineNumberReader);
                closeQuietly(parserIs);
            }

            if (outboundPayload.size() > 0) {
                callback.sendTextMessage(headers, outboundPayload);
            }
        }
    }

    protected static void forward(int toLine, LineNumberReader lineNumberReader) throws IOException {
        while (lineNumberReader.getLineNumber() < toLine) {
            String output = lineNumberReader.readLine();
            if (output == null) {
                break;
            }
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    public void setRunWhen(String runWhen) {
        this.runWhen = runWhen;
    }
}
