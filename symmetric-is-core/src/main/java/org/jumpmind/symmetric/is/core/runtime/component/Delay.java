package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.util.AppUtils;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = Delay.TYPE, inputMessage=MessageType.ANY,
outgoingMessage=MessageType.ANY, iconImage="timer.png")
public class Delay extends AbstractComponentRuntime {

    public static final String TYPE = "Delay";
    
    @SettingDefinition(order = 1, required = true, type = Type.INTEGER, defaultValue = "1000", label = "Delay (ms)")
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
        messageTarget.put(inputMessage.copy(getFlowStepId()));
    }

}
