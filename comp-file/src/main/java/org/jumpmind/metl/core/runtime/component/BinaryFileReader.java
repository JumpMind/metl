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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class BinaryFileReader extends AbstractFileReader {

    public static final String TYPE = "Binary File Reader";
    public static final String SETTING_SIZE_PER_MESSAGE = "size.per.message";

    int sizePerMessage = 100;
    @Override
    public void start() {
    	init();
        Component component = getComponent();
        sizePerMessage = component.getInt(SETTING_SIZE_PER_MESSAGE, sizePerMessage);
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

        filesRead.addAll(files);

        for (String file : files) {
            Map<String, Serializable> headers = new HashMap<>(1);
            headers.putAll(inputMessage.getHeader());
            headers.put("source.file.path", file);
            InputStream inStream = null;
            try {
                if (isNotBlank(file)) {
                    info("Reading file: %s", file);
                }
                String filePath = resolveParamsAndHeaders(file, inputMessage);
                inStream = directory.getInputStream(filePath, mustExist);
                //TODO: if the file is bigger than the allowable message size, this doesn't work
                if (inStream != null) {
                    byte[] payload = IOUtils.toByteArray(inStream);
                    callback.sendBinaryMessage(headers, payload);
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);               
                } else {
                    if (isNotBlank(file)) {
                        info("File %s didn't exist, but must exist setting was false.  Continuing", file);
                    }
                }
            } catch (IOException e) {
                throw new IoException("Error reading from file " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(inStream);
            }
            
            if (controlMessageOnEof) {
            	callback.sendControlMessage(headers);
            }
        }
    }
}
