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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;

public class Union extends AbstractComponentRuntime {

    public static final String TYPE = "Union";
    
    public final static String SETTING_ERROR_ON_MISMATCH_MESSAGES = "error.on.mismatch.messages";
    
    boolean errorOnMismatchMessages = false;

    List<FlowStepLink> flowStepLinks;
    
    Map<String, List<Message>> messagesByFlowStep;
    Map<String, Integer> messageCountByFlowStep;

    @Override
    protected void start() {
    	TypedProperties properties = getTypedProperties();
    	errorOnMismatchMessages = properties.is(SETTING_ERROR_ON_MISMATCH_MESSAGES, errorOnMismatchMessages);

    	flowStepLinks = getFlow().findFlowStepLinksWithTarget(getFlowStepId());
        messagesByFlowStep = new HashMap<String, List<Message>>();
        messageCountByFlowStep = new HashMap<String, Integer>();
        for (FlowStepLink flowStepLink : flowStepLinks) {
        	FlowStep sourceFlowStep = getFlow().findFlowStepWithId(flowStepLink.getSourceStepId());
            Component sourceComponent = sourceFlowStep.getComponent();
            if (sourceComponent.getBoolean(AbstractComponentRuntime.ENABLED, true)) {
                messagesByFlowStep.put(flowStepLink.getSourceStepId(), new ArrayList<Message>());
                messageCountByFlowStep.put(flowStepLink.getSourceStepId(), 0);
            }
        }
    }
        
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }
    
    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        String fromStepId = inputMessage.getHeader().getOriginatingStepId();
        List<Message> messages = messagesByFlowStep.get(fromStepId);
        if (messages != null) {
            messages.add(inputMessage);
        }
        Integer messageCount = messageCountByFlowStep.get(fromStepId);
        messageCountByFlowStep.put(fromStepId, (messageCount == null) ? 1 : messageCount + 1);

        if (unitOfWorkBoundaryReached && errorOnMismatchMessages)
        {
        	int previousMessageCount = -1;
        	for (Integer countMessages : messageCountByFlowStep.values()) {
        		if (previousMessageCount == -1)
        		{
        			previousMessageCount = countMessages;
        		}
        		else
        		{
        			if (previousMessageCount != countMessages) {
        				throw new IllegalStateException("Number of messages received on each input link do not match.");
        			}
        		}
            }
        }
        
        boolean readyToProcess = true;
        boolean isLastUnitOfWorkMessage = true;
        for (List<Message> unhandledMessages : messagesByFlowStep.values()) {
        	isLastUnitOfWorkMessage &= unhandledMessages.size() > 0;
        	for (Message unhandledMessage : unhandledMessages) {
    			isLastUnitOfWorkMessage &= unhandledMessage instanceof ControlMessage;
        	}
        	 
            readyToProcess &= unhandledMessages.size() > 0;
        }

        if (readyToProcess && !isLastUnitOfWorkMessage) {
            Message outputMessage = new Message(getFlowStepId());
            outputMessage.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
            ArrayList<EntityData> rowData = new ArrayList<EntityData>();
            outputMessage.setPayload(rowData);
            for (List<Message> unhandledMessages : messagesByFlowStep.values()) {
            	Message unhandledMessage = unhandledMessages.get(0);
            	if (unhandledMessage != null && !(unhandledMessage instanceof ControlMessage)) {
                    Message message = unhandledMessages.remove(0);
                    ArrayList<EntityData> inputRowData = message.getPayload();
                    rowData.addAll(inputRowData);
                    getComponentStatistics().incrementNumberEntitiesProcessed(inputRowData.size());
            	}
            }

            callback.sendMessage(null, outputMessage.getPayload());
        }

        if (isLastUnitOfWorkMessage) {
            callback.sendMessage(null, inputMessage.getPayload());
        }
       
    }

}
