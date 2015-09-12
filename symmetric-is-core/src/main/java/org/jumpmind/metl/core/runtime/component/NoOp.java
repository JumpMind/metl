package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

public class NoOp extends AbstractComponentRuntime {

    public static final String TYPE = "No Op";

    @Override
    protected void start() {
    }
    
    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
