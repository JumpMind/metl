package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.AppUtils;

public class Delay extends AbstractComponentRuntime {

    public static final String TYPE = "Delay";
    
    public final static String DELAY_TIME = "delay.in.ms";
    
    long delay = 1000;

    @Override
    protected void start() {        
        delay = getComponent().getLong(DELAY_TIME, 1000l);
    }
    
    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        AppUtils.sleep(delay);
        callback.sendMessage(inputMessage.getPayload(), unitOfWorkLastMessage);
    }

}
