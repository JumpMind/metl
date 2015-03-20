package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = NoOpProcessor.TYPE, inputMessage=MessageType.ENTITY_MESSAGE,
outgoingMessage=MessageType.ENTITY_MESSAGE)
public class NoOpProcessor extends AbstractComponent {

    public static final String TYPE = "Entity No Op";

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        componentStatistics.incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
