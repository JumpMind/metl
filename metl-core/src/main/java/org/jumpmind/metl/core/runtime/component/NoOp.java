package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class NoOp extends AbstractComponentRuntime {

    public static final String TYPE = "No Op";

    @Override
    protected void start() {
    }
    
    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
