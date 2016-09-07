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
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.Http;
import org.jumpmind.metl.core.runtime.resource.HttpOutputStream;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.util.FormatUtils;

public class Web extends AbstractComponentRuntime {

    public static final String TYPE = "Web";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String RELATIVE_PATH = "relative.path";
    
    public static final String BODY_FROM = "body.from";
    
    public static final String BODY_TEXT = "body.text";
    
    public static final String PARAMETER_REPLACEMENT = "parameter.replacement";
    
    String runWhen;

    String relativePath;
    
    String bodyFrom;
    
    String bodyText;
    
    boolean parameterReplacement;
    
    @Override
    public void start() {
        IResourceRuntime httpResource = getResourceRuntime();
        if (httpResource == null || !(httpResource instanceof Http)) {
            throw new IllegalStateException(String.format(
                    "A msgTarget resource of type %s must be chosen.  Please choose a resource.",
                    Http.TYPE));
        }

        Component component = getComponent();
        relativePath = FormatUtils.replaceTokens(component.get(RELATIVE_PATH),
                context.getFlowParameters(), true);
        bodyFrom = component.get(BODY_FROM, "Message");
        bodyText = component.get(BODY_TEXT);
        parameterReplacement = component.getBoolean(PARAMETER_REPLACEMENT, false);
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

			ArrayList<String> outputPayload = new ArrayList<String>();
			ArrayList<String> inputPayload = new ArrayList<String>();
			if (bodyFrom.equals("Message") && inputMessage instanceof TextMessage) {
				inputPayload = ((TextMessage)inputMessage).getPayload();
			} else {
    		    inputPayload.add(bodyText);
			}

			if (inputPayload != null) {
				try {
					for (String requestContent : inputPayload) {
						getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
						if (parameterReplacement) {
							requestContent = resolveParamsAndHeaders(requestContent, inputMessage);
						}
						
                        if (isNotBlank(requestContent)) {
                            HttpOutputStream os = (HttpOutputStream) streamable
                                    .getOutputStream(relativePath, false);
                            BufferedWriter writer = new BufferedWriter(
                                    new OutputStreamWriter(os, DEFAULT_CHARSET));
                            try {
                                writer.write(requestContent);
                            } finally {
                                writer.close();
                                String response = os.getResponse();
                                if (response != null) {
                                    outputPayload.add(response);
                                }
                            }
                        } else {
                            InputStream is = streamable.getInputStream(relativePath, false);
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
}
