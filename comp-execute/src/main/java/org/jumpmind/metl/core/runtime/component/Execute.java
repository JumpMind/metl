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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.symmetric.csv.CsvReader;

public class Execute extends AbstractComponentRuntime {

    public static final String TYPE = "Execute";

    public final static String COMMAND = "command";

    public final static String CONTINUE_ON_ERROR = "continue.on.error";

    public final static String SUCCESS_CODE = "success.code";
    
    public static final String PARAMETER_REPLACEMENT = "parameter.replacement";
    
    String runWhen = PER_UNIT_OF_WORK;

    boolean continueOnError = false;
    
    boolean parameterReplacement;

    int successCode = 0;

    @Override
    public void start() {
        String line = getComponent().get(COMMAND, null);
        continueOnError = getComponent().getBoolean(CONTINUE_ON_ERROR, continueOnError);
        successCode = getComponent().getInt(SUCCESS_CODE, successCode);
        runWhen = getComponent().get(RUN_WHEN, PER_UNIT_OF_WORK);

        if (isBlank(line)) {
            throw new IllegalStateException("A command is required by this component");
        }        
        parameterReplacement = getComponent().getBoolean(PARAMETER_REPLACEMENT, false);
    }    
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }
    
    protected String[] parseCommand(String commandLine, Message inputMessage) {
        try {
            String[] commands = null;
            CsvReader csvReader = new CsvReader(new ByteArrayInputStream(commandLine.getBytes()), Charset.forName("utf-8"));
            csvReader.setDelimiter(' ');
            csvReader.setTextQualifier('"');
            csvReader.setUseTextQualifier(true);
            csvReader.setEscapeMode(CsvReader.ESCAPE_MODE_BACKSLASH);
            if (csvReader.readRecord()) {
                commands = csvReader.getValues();
            }
            
            if (commands != null) {
                if (parameterReplacement) {
                    for(int i = 0; i < commands.length; i++) {
                        commands[i] = resolveParamsAndHeaders(commands[i], inputMessage);
                    }
                }
            }
            return commands;
        } catch (Exception e) {
            throw new IoException(e);
        }
        
    }

	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {		
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			try {
				ByteArrayOutputStream os = getByteArrayOutputStream();
				PumpStreamHandler outputHandler = new PumpStreamHandler(os);
				org.apache.tools.ant.taskdefs.Execute antTask = getAntTask(outputHandler);
				String[] commands = parseCommand(getComponent().get(COMMAND), inputMessage);
				antTask.setCommandline(commands);
				info("About to execute: %s", ArrayUtils.toString(commands));
				int code = antTask.execute();				
				String output = new String(os.toByteArray());
				if (successCode == code || continueOnError) {
					if (successCode == code) {
						info("Returned an code of %d", code);
					} else {
						warn("Returned an code of %d", code);
					}
					info("The output of the command was: %s", output);

					ArrayList<String> payload = new ArrayList<String>();
					payload.add(output);
					callback.sendTextMessage(null, payload);
				} else {
					info("The output of the command was: %s", output);
					throw new IoException("%s failed with an error code of %d", ArrayUtils.toString(commands), code);
				}
			} catch (IOException e) {
				throw new IoException(e);
			}
		}
	}

    org.apache.tools.ant.taskdefs.Execute getAntTask(PumpStreamHandler outputHandler) {
    	return new org.apache.tools.ant.taskdefs.Execute(outputHandler);
    }
    
    ByteArrayOutputStream getByteArrayOutputStream() {
    	return new ByteArrayOutputStream();
    }
    
    public void setRunWhen(String runWhen) {
        this.runWhen = runWhen;
    }

}
