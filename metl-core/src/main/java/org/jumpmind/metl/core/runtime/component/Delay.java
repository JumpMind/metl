package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;
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
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        AppUtils.sleep(delay);
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(inputMessage.clone(getFlowStepId()));
    }

}
