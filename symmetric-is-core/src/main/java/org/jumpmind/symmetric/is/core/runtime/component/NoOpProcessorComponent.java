package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = NoOpProcessorComponent.type, supports = {
        ComponentSupports.INPUT_MESSAGE, ComponentSupports.OUTPUT_MESSAGE })

public class NoOpProcessorComponent extends AbstractComponent {

	public static final String type="No Op";
		
	@Override
	public void handle(Message inputMessage, IMessageTarget messageTarget) {
		componentStatistics.incrementInboundMessages();
		messageTarget.put(inputMessage);	
		messageTarget.put(new ShutdownMessage());
	}

}
