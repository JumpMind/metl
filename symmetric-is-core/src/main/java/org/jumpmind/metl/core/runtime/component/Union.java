package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

public class Union extends AbstractComponentRuntime {

    public static final String TYPE = "Union";

    List<FlowStepLink> flowStepLinks;
    
    Map<String, List<Message>> messagesByFlowStep;
    
    @Override
    protected void start() {
        flowStepLinks = getFlow().findFlowStepLinksWithTarget(getFlowStepId());
        messagesByFlowStep = new HashMap<String, List<Message>>();
        for (FlowStepLink flowStepLink : flowStepLinks) {
            messagesByFlowStep.put(flowStepLink.getSourceStepId(), new ArrayList<Message>());
        }
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        String fromStepId = inputMessage.getHeader().getOriginatingStepId();
        List<Message> messages = messagesByFlowStep.get(fromStepId);
        if (messages != null) {
            messages.add(inputMessage);
        }

        boolean readyToProcess = true;
        for (List<Message> unhandledMessages : messagesByFlowStep.values()) {
            readyToProcess &= unhandledMessages.size() > 0;
        }
        
        if (readyToProcess) {
            Message outputMessage = new Message(getFlowStepId());
            outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
            outputMessage.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
            ArrayList<EntityData> rowData = new ArrayList<EntityData>();
            outputMessage.setPayload(rowData);
            for (List<Message> unhandledMessages : messagesByFlowStep.values()) {
                Message message = unhandledMessages.remove(0);
                ArrayList<EntityData> inputRowData = message.getPayload();
                rowData.addAll(inputRowData);
                getComponentStatistics().incrementNumberEntitiesProcessed(inputRowData.size());
            }
            getComponentStatistics().incrementOutboundMessages();
            messageTarget.put(outputMessage);
        }
    }

}
