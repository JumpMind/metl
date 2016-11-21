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
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class TextConstant extends AbstractComponentRuntime {

    public static final String TYPE = "Text Constant";

    public static final String SETTING_SPLIT_ON_LINE_FEED = "split.on.line.feed";
    
    public static final String SETTING_CONTROL_MESSAGE_ON_TEXT_SEND = "control.message.on.text.send";

    public static final String SETTING_TEXT = "text";
    
    String runWhen = PER_UNIT_OF_WORK;

    int textRowsPerMessage;
    boolean splitOnLineFeed;
    boolean controlMessageOnTextSend;
    String constantText;
    
    @Override
    public void start() {
    	textRowsPerMessage = context.getFlowStep().getComponent().getInt(ROWS_PER_MESSAGE, 1000);
        splitOnLineFeed = context.getFlowStep().getComponent().getBoolean(SETTING_SPLIT_ON_LINE_FEED, true);
        constantText = context.getFlowStep().getComponent().get(SETTING_TEXT, "");
        controlMessageOnTextSend = context.getFlowStep().getComponent().getBoolean(SETTING_CONTROL_MESSAGE_ON_TEXT_SEND, false);
        runWhen = getComponent().get(RUN_WHEN, PER_UNIT_OF_WORK);
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {

		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			int linesInMessage = 0;
			ArrayList<String> payload = new ArrayList<String>();

			if (!splitOnLineFeed) {
				getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
				payload.add(constantText);
			} else {
				BufferedReader reader = null;
				String currentLine;
				try {
					reader = new BufferedReader(new StringReader(constantText));
					while ((currentLine = reader.readLine()) != null) {
						if (linesInMessage == textRowsPerMessage) {
							callback.sendTextMessage(null, payload);
							linesInMessage = 0;
							payload = new ArrayList<String>();
						}
						getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
						payload.add(currentLine);
						linesInMessage++;
					}
				} catch (IOException e) {
					throw new IoException("Error reading from file " + e.getMessage());
				} finally {
					IOUtils.closeQuietly(reader);
				}
			}

			if (payload.size() > 0) {
			    callback.sendTextMessage(null, payload);
			    if (PER_MESSAGE.equals(runWhen) && controlMessageOnTextSend) {
			        callback.sendControlMessage();
			    }
			}
			
		}
	}
	
	public void setRunWhen(String runWhen) {
        this.runWhen = runWhen;
    }
}
