package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, 
typeName = Multiplier.TYPE, 
iconImage = "multiplier.png",
inputMessage=MessageType.ENTITY,
outgoingMessage=MessageType.ENTITY)
public class Multiplier extends AbstractComponent {

    public static final String TYPE = "Multiplier";

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        
        // create script engine for use in handle
        
        // read json configuration
    }
    
    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        componentStatistics.incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
