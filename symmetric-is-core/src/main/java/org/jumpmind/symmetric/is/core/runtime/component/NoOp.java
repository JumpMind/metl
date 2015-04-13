package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = NoOp.TYPE, inputMessage=MessageType.ANY,
outgoingMessage=MessageType.ANY)
public class NoOp extends AbstractComponent {

    public static final String TYPE = "No Op";

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        componentStatistics.incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
