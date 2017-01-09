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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IOutputStreamWithResponse;
//import org.jumpmind.metl.core.runtime.resource.Http;
//import org.jumpmind.metl.core.runtime.resource.HttpDirectory;
//import org.jumpmind.metl.core.runtime.resource.HttpOutputStream;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.util.FormatUtils;

public class Web extends AbstractComponentRuntime {

    public static final String TYPE = "Web";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String RELATIVE_PATH = "relative.path";
    
    public static final String BODY_FROM = "body.from";
    
    public static final String BODY_TEXT = "body.text";
    
    public static final String HTTP_HEADERS = "http.headers";
    
    public static final String HTTP_PARAMETERS = "http.parameters";
    
    public static final String PARAMETER_REPLACEMENT = "parameter.replacement";
    
    String runWhen;
    
    String relativePath;
    
    String bodyFrom;
    
    String bodyText;
    
    Map<String,String> httpHeaders;
    
    Map<String,String> httpParameters;
    
    boolean parameterReplacement;
    
    @Override
    public void start() {
        IResourceRuntime httpResource = getResourceRuntime();
        if (httpResource == null) {
            throw new IllegalStateException("An HTTP resource must be configured");
        }
        Component component = getComponent();
        bodyFrom = component.get(BODY_FROM, "Message");
        bodyText = component.get(BODY_TEXT);
        runWhen = getComponent().get(RUN_WHEN, PER_MESSAGE);
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {	    
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			IDirectory streamable = getResourceReference();
			httpHeaders = getHttpHeaderConfigEntries(inputMessage);
			httpParameters = getHttpParameterConfigEntries(inputMessage);
			assembleRelativePathPlusParameters();
			ArrayList<String> outputPayload = new ArrayList<String>();
			ArrayList<String> inputPayload = new ArrayList<String>();
			if (bodyFrom.equals("Message") && inputMessage instanceof TextMessage) {
				inputPayload = ((TextMessage)inputMessage).getPayload();
			} else {
    		    inputPayload.add(bodyText);
			}

			if (inputPayload != null) {
				try {
				    String path = resolveParamsAndHeaders(relativePath,inputMessage);
					for (String requestContent : inputPayload) {
						getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
						if (parameterReplacement) {
							requestContent = resolveParamsAndHeaders(requestContent, inputMessage);
						}
						
                        if (isNotBlank(requestContent)) {
                            info("sending content to %s", path);
                            OutputStream os = streamable
                                    .getOutputStream(path, false, false, false, httpHeaders, httpParameters);
                            BufferedWriter writer = new BufferedWriter(
                                    new OutputStreamWriter(os, DEFAULT_CHARSET));
                            try {
                                writer.write(requestContent);
                            } finally {
                                writer.close();
                                if (os instanceof IOutputStreamWithResponse) {
                                    String response = ((IOutputStreamWithResponse) os).getResponse();
                                    if (response != null) {
                                        outputPayload.add(response);
                                    }
                                }
                            }
                        } else {
                            info("getting content from %s", path);
                            InputStream is = streamable.getInputStream(path, false, false, httpHeaders, httpParameters);
                            try {
                                String response = IOUtils.toString(is);
                                if (response != null) {
                                    outputPayload.add(response);
                                }
                            } finally {
                                IOUtils.closeQuietly(is);
                            }
                        }
					}

					if (outputPayload.size() > 0) {
						callback.sendTextMessage(null, outputPayload);
					}
				} catch (IOException e) {
					throw new IoException(String.format("Error writing to %s ", streamable), e);
				}
			}
		}
	}
	
    private Map<String, String> getHttpHeaderConfigEntries(Message inputMessage) {
        String headersText = resolveParamsAndHeaders(properties.get(HTTP_HEADERS), inputMessage);
        return parseDelimitedMultiLineParamsToMap(headersText, inputMessage);
    }

    private Map<String, String> getHttpParameterConfigEntries(Message inputMessage) {
        String parametersText = resolveParamsAndHeaders(properties.get(HTTP_PARAMETERS), inputMessage);
        return parseDelimitedMultiLineParamsToMap(parametersText, inputMessage);
    }   
    
    private Map<String, String> parseDelimitedMultiLineParamsToMap(String parametersText, Message inputMessage) {
        Map<String, String> parsedMap = new HashMap<>();
        if (parametersText != null) {
            String[] parameters = parametersText.split(System.getProperty("line.separator"));
            for (String parameter : parameters) {
                String[] pair = parameter.split(":");
                if (pair != null && pair.length > 1) {
                    parsedMap.put(pair[0], pair[1]);
                }
            }
        }
        return parsedMap;
    }
    
    private void assembleRelativePathPlusParameters() {
        Component component = getComponent();
        String basePath = FormatUtils.replaceTokens(component.get(RELATIVE_PATH),
                context.getFlowParameters(), true);
        relativePath = basePath;
        int parmCount = 0;
        for (Map.Entry<String, String> entry : httpParameters.entrySet()) {
            parmCount++;
            if (parmCount == 1) {
                relativePath = relativePath + "?"; 
            } else {
                relativePath = relativePath + "&";
            }
            try {
                relativePath = relativePath + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), DEFAULT_CHARSET);
            } catch(UnsupportedEncodingException e) {
                log.error("Error URL Encoding parameters");
                throw new RuntimeException(e);
            }
        }
    }
}
