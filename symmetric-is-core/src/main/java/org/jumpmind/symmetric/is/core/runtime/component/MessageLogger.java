package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;

import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.MessageHeader;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = MessageLogger.TYPE,
        inputMessage = MessageType.ANY,
        outgoingMessage = MessageType.ANY,
        inputOutputModelsMatch=true,
        iconImage = "log.png")
public class MessageLogger extends AbstractComponent {

    public static final String TYPE = "Message Logger";

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();

        MessageHeader header = inputMessage.getHeader();
        executionTracker.log(executionId, LogLevel.DEBUG, this, String.format(
                "Message(sequenceNumber=%d,last=%s,source='%s')", header.getSequenceNumber(),
                header.isLastMessage(), flow.findFlowStepWithId(header.getOriginatingStepId())
                        .getName()));
        Object payload = inputMessage.getPayload();
        if (payload instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) payload;
            for (Object object : list) {
                if (object instanceof EntityData && flowStep.getComponent().getInputModel() != null) {
                    executionTracker.log(
                            executionId,
                            LogLevel.DEBUG,
                            this,
                            String.format("Message Payload: %s",
                                    flowStep.getComponent().toRow((EntityData) object)));
                } else {
                    executionTracker.log(executionId, LogLevel.DEBUG, this,
                            String.format("Message Payload: %s", object));
                }
            }
        }
        componentStatistics.incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
