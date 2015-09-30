package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class LastUnitOfWork extends AbstractComponentRuntime {

    public static final String TYPE = "Last Unit of Work";
    
    @Override
    protected void start() {        
    }
    
    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();        
        if (unitOfWorkLastMessage) {
        	callback.sendStartupMessage();
        }
    }

}
