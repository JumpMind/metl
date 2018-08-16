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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class TextFileReader extends AbstractFileReader {

    public static final String TYPE = "Text File Reader";

    public static final String SETTING_ROWS_PER_MESSAGE = "text.rows.per.message";

    public static final String SETTING_ENCODING = "encoding";

    public static final String SETTING_HEADER_LINES_TO_SKIP = "text.header.lines.to.skip";

    public static final String SETTING_NUMBER_OF_TIMES_TO_READ_FILE = "number.of.times.to.read.file";

    public static final String SETTING_SPLIT_ON_LINE_FEED = "split.on.line.feed";

    int textRowsPerMessage = 1000;

    int numberOfTimesToReadFile = 1;

    int textHeaderLinesToSkip;

    String encoding = "UTF-8";

    @Override
    public void start() {
        init();
        TypedProperties properties = getTypedProperties();
        textHeaderLinesToSkip = properties.getInt(SETTING_HEADER_LINES_TO_SKIP, textHeaderLinesToSkip);
        textRowsPerMessage = properties.getInt(SETTING_ROWS_PER_MESSAGE, textRowsPerMessage);
        numberOfTimesToReadFile = properties.getInt(SETTING_NUMBER_OF_TIMES_TO_READ_FILE, numberOfTimesToReadFile);
        encoding = properties.get(SETTING_ENCODING, encoding);
        if ("".equals(encoding)) {
        	encoding = "UTF-8";
        	log(LogLevel.INFO, "File Encoding has not been set, using the default of UTF-8.");
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
                || (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
            List<String> files = getFilesToRead(inputMessage);
            processFiles(files, inputMessage, callback, unitOfWorkBoundaryReached);
        }
    }

    private void processFiles(List<String> files, Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        int linesInMessage = 0;
        ArrayList<String> payload = new ArrayList<String>();

        filesRead.addAll(files);

        for (String file : files) {
            Map<String, Serializable> headers = new HashMap<>(1);
            headers.putAll(inputMessage.getHeader());
            headers.put("source.file.path", file);
            int currentFileLinesRead = 0;
            String currentLine;
            boolean readContent = true;
            if (directory == null) {
                throw new IllegalStateException("The resource was not created.  Please check to see that it is properly configured");
            }
            try {
                for (int i = 0; i < numberOfTimesToReadFile && readContent; i++) {
                    checkForInterruption();
                    if (isNotBlank(file)) {
                        info("Reading file: %s", file);
                    }
                    String filePath = resolveParamsAndHeaders(file, inputMessage);
                    BufferedReader reader = null;
                    try {
                        InputStream inStream = directory.getInputStream(filePath, mustExist);
                        if (inStream != null) {
                            reader = new BufferedReader(new InputStreamReader(inStream, encoding));
                            if (properties.is(SETTING_SPLIT_ON_LINE_FEED, true)) {
                                while ((currentLine = reader.readLine()) != null) {
                                    currentFileLinesRead++;
                                    if (linesInMessage == textRowsPerMessage) {
                                        callback.sendTextMessage(headers, payload);
                                        linesInMessage = 0;
                                        payload = new ArrayList<String>();
                                    }
                                    if (currentFileLinesRead > textHeaderLinesToSkip) {
                                        getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                                        payload.add(currentLine);
                                        linesInMessage++;
                                    }
                                }
                            } else {
                                payload.add(IOUtils.toString(reader));
                            }
                            if (payload.size() > 0) {
                                callback.sendTextMessage(headers, payload);
                                payload = new ArrayList<>();
                            } else {
                                readContent = false;
                            }
                            linesInMessage = 0;
                        } else {
                            if (isNotBlank(file)) {
                                info("File %s didn't exist, but must exist setting was false.  Continuing", file);
                            } else {
                                info("There was no content to read");
                            }
                            readContent = false;
                        }
                    } finally {
                        // Closes the reader and the inStream.
                        IOUtils.closeQuietly(reader);
                    }
                }
            } catch (IOException e) {
                throw new IoException("Error reading from file " + e.getMessage());
            }

            if (controlMessageOnEof) {
                callback.sendControlMessage(headers);
            }
        }
    }
}
