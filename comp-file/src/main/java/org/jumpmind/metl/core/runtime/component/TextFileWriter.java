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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.properties.TypedProperties;

public class TextFileWriter extends AbstractFileWriter {

    public final static String TYPE = "Text File Writer";

    public final static String DEFAULT_ENCODING = "UTF-8";

    public final static String SETTING_ENCODING = "encoding";
    
    public final static String SETTING_APPEND = "append";

    public final static String SETTING_EMPTY_FILE = "empty.file";
    
    public final static String SETTING_CLOSE_ON = "close.on";

    public static final String SETTING_TEXT_LINE_TERMINATOR = "text.line.terminator";
    
    public static final String CLOSE_ON_UNIT_OF_WORK = "UNIT OF WORK";
    
    public static final String CLOSE_ON_MESSAGE = "MESSAGE";
    
    public static final String CLOSE_ON_ROW = "ROW";

    String encoding;

    String lineTerminator;
    
    boolean append;
    
    boolean emptyFile;
    
    boolean inputDataReceived = false;
    
    String closeOn = CLOSE_ON_UNIT_OF_WORK;

    BufferedWriter bufferedWriter = null;

    @Override
    public void start() {
    	init();
        TypedProperties properties = getTypedProperties();
        lineTerminator = properties.get(SETTING_TEXT_LINE_TERMINATOR);
        encoding = properties.get(SETTING_ENCODING, DEFAULT_ENCODING); 
        append = properties.is(SETTING_APPEND, false);
        emptyFile = properties.is(SETTING_EMPTY_FILE, false);
        closeOn = properties.get(SETTING_CLOSE_ON, closeOn);               
        if (lineTerminator != null) {
            lineTerminator = StringEscapeUtils.unescapeJava(properties.get(SETTING_TEXT_LINE_TERMINATOR));
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("The msgTarget resource has not been configured.  Please choose a resource.");
        }
        if ((unitOfWorkBoundaryReached && inputDataReceived == false && emptyFile == true) ||
        		(inputMessage instanceof ContentMessage<?>)) {
        	inputDataReceived = true;
	        initStreamAndWriter(inputMessage);
	        
	        if (inputMessage instanceof ContentMessage<?>) {
		        try {
		            Object payload = ((ContentMessage<?>)inputMessage).getPayload();
		            if (payload instanceof ArrayList) {
		                ArrayList<?> recs = (ArrayList<?>) payload;
		                for (Object rec : recs) {
		                    bufferedWriter.write(rec != null ? rec.toString() : "");
		                    if (lineTerminator != null) {
		                        bufferedWriter.write(lineTerminator);
		                    } else {
		                        bufferedWriter.newLine();
		                    }
		                }
		
		            } else if (payload instanceof String) {
		                bufferedWriter.write((String) payload);
		            } else {
		                bufferedWriter.write("");
		            }
		            
                    bufferedWriter.flush();
                    if (CLOSE_ON_ROW.equals(closeOn)) {
                        close();
                        initStreamAndWriter(inputMessage);
                    }
		        } catch (IOException e) {
		            throw new IoException(e);
		        }
	        }
        }
        
        if ((inputMessage instanceof ControlMessage ||
        		unitOfWorkBoundaryReached) && callback != null) {
            close();
            ArrayList<String> results = new ArrayList<>(1);
            results.add("{\"status\":\"success\"}");
            callback.sendTextMessage(null, results);
        } else if (CLOSE_ON_MESSAGE.equals(closeOn)) {
            close();
        } 
        

    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        close();
        super.flowCompletedWithErrors(myError);
    }

    private void initStreamAndWriter(Message inputMessage) {
    	if (bufferedWriter == null) {
	    	String fileName = getFileName(inputMessage);
	    	IDirectory streamable = initStream(fileName);
	        log(LogLevel.INFO, String.format("Writing text file to resource: %s with name: %s", streamable.toString(), fileName));
	       	bufferedWriter = initializeWriter(streamable.getOutputStream(fileName, mustExist, false, append));
    	}
    }

    private BufferedWriter initializeWriter(OutputStream stream) {
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(stream, encoding));
        } catch (UnsupportedEncodingException e) {
            throw new IoException("Error creating buffered writer " + e.getMessage());
        }
        return bufferedWriter;
    }

    private void close() {
        IOUtils.closeQuietly(bufferedWriter);
        bufferedWriter = null;
    }

}
