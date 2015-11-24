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
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.util.FormatUtils;

public class TextFileReader extends AbstractFileReader {

    public static final String TYPE = "Text File Reader";

    public static final String SETTING_ROWS_PER_MESSAGE = "textfilereader.text.rows.per.message";

    public final static String SETTING_ENCODING = "textfilereader.encoding";

    public static final String SETTING_HEADER_LINES_TO_SKIP = "textfilereader.text.header.lines.to.skip";

    int textRowsPerMessage = 10000;

    int textHeaderLinesToSkip;

    String encoding = "UTF-8";
    
    @Override
    protected void start() {
    	init();
    	//TODO: getTypedProperties
        Component component = getComponent();
        textHeaderLinesToSkip = component.getInt(SETTING_HEADER_LINES_TO_SKIP, textHeaderLinesToSkip);
        textRowsPerMessage = component.getInt(SETTING_ROWS_PER_MESSAGE, textRowsPerMessage);
        encoding = component.get(SETTING_ENCODING, encoding);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			List<String> files = getFilesToRead(inputMessage);
    		processFiles(files, callback, unitOfWorkBoundaryReached);
    	}
    }

    private void processFiles(List<String> files, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        int linesInMessage = 0;
        ArrayList<String> payload = new ArrayList<String>();

        filesRead.addAll(files);

        for (String file : files) {
            Map<String, Serializable> headers = new HashMap<>(1);
            headers.put("source.file.path", file);
            InputStream inStream = null;
            BufferedReader reader = null;
            int currentFileLinesRead = 0;
            String currentLine;
            try {
                info("Reading file: %s", file);
                IDirectory resource = (IDirectory) getResourceReference();
                String filePath = FormatUtils.replaceTokens(file, context.getFlowParametersAsString(), true);
                inStream = resource.getInputStream(filePath, mustExist);
                reader = new BufferedReader(new InputStreamReader(inStream, encoding));

                while ((currentLine = reader.readLine()) != null) {
                    currentFileLinesRead++;
                    if (linesInMessage == textRowsPerMessage) {
                        callback.sendMessage(headers, payload);
                        linesInMessage = 0;
                        payload = new ArrayList<String>();
                    }
                    if (currentFileLinesRead > textHeaderLinesToSkip) {
                        getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                        payload.add(currentLine);
                        linesInMessage++;
                    }
                }
                if (payload.size() > 0) {
                    callback.sendMessage(headers, payload);
                }
                linesInMessage = 0;
            } catch (IOException e) {
                throw new IoException("Error reading from file " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(inStream);
            }
        }

    }
}
