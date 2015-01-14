package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = NoOpProcessorComponent.TYPE, supports = {
        ComponentSupports.INPUT_MESSAGE, ComponentSupports.OUTPUT_MESSAGE })

public class NoOpProcessorComponent extends AbstractComponent {

	
	//TODO: this shouldnt send shutdown msg unless he is startupnode or he has recieved a shutdown msg from someone else
	public static final String TYPE="No Op";
		
	@Override
	public void handle(Message inputMessage, IMessageTarget messageTarget) {
		componentStatistics.incrementInboundMessages();
		messageTarget.put(inputMessage);	
		messageTarget.put(new ShutdownMessage());
	}

}
