package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.util.AppUtils;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = Delay.TYPE, inputMessage=MessageType.ANY_MESSAGE,
outgoingMessage=MessageType.ANY_MESSAGE, iconImage="timer.png")
public class Delay extends AbstractComponent {

    public static final String TYPE = "Delay";
    
    @SettingDefinition(order = 1, required = true, type = Type.INTEGER, defaultValue = "1000", label = "Delay (ms)")
    public final static String DELAY_TIME = "delay.in.ms";
    
    long delay = 1000;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        delay = flowStep.getComponent().getLong(DELAY_TIME, 1000l);
    }
    
    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        AppUtils.sleep(delay);
        componentStatistics.incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
