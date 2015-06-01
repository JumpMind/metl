package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;

import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.MessageHeader;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = MessageLogger.TYPE,
        inputMessage = MessageType.ANY,
        outgoingMessage = MessageType.ANY,
        inputOutputModelsMatch = true,
        iconImage = "log.png")
public class MessageLogger extends AbstractComponentRuntime {

    public static final String TYPE = "Message Logger";

    @Override
    protected void start() {
    }
    
    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();

        MessageHeader header = inputMessage.getHeader();
        log(LogLevel.DEBUG, String.format("Message(sequenceNumber=%d,last=%s,source='%s')",
                header.getSequenceNumber(), header.isLastMessage(),
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
                                    getComponent().toRow((EntityData) object)));
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
