package org.jumpmind.metl.core.runtime.component;

import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MessageHeader;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class MessageLogger extends AbstractComponentRuntime {

    public static final String TYPE = "Message Logger";

    @Override
    protected void start() {
    }
    
    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();

        MessageHeader header = inputMessage.getHeader();
        log(LogLevel.DEBUG, String.format("Message(sequenceNumber=%d,last=%s,source='%s')",
                header.getSequenceNumber(), header.isUnitOfWorkLastMessage(),
                getFlow().findFlowStepWithId(header.getOriginatingStepId()).getName()));
        Object payload = inputMessage.getPayload();
        if (payload instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) payload;
            for (Object object : list) {
                if (object instanceof EntityData && getComponent().getInputModel() != null) {
                    getComponentStatistics().incrementNumberEntitiesProcessed();
                    log(LogLevel.DEBUG,
                            String.format("Message Payload: %s",
                                    getComponent().toRow((EntityData) object, false)));
                } else {
                    getComponentStatistics().incrementNumberEntitiesProcessed();
                    log(LogLevel.DEBUG, String.format("Message Payload: %s", object));
                }
            }
        }
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
