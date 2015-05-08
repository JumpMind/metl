package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = NoOp.TYPE, inputMessage=MessageType.ANY,
outgoingMessage=MessageType.ANY)
public class NoOp extends AbstractComponentRuntime {

    public static final String TYPE = "No Op";

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
