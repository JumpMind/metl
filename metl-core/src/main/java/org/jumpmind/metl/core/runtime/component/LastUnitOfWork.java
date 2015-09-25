package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class LastUnitOfWork extends AbstractComponentRuntime {

    public static final String TYPE = "Last Unit of Work";
    
    @Override
    protected void start() {        
    }
    
    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();        
        if (unitOfWorkLastMessage) {
        	Message msg = new StartupMessage();
        	msg.getHeader().setOriginatingStepId(getFlowStepId());
        	messageTarget.put(msg);
            getComponentStatistics().incrementOutboundMessages();
        }
    }

}
