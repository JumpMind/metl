package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = NoOpProcessor.TYPE, supports = {
        ComponentSupports.BINARY_INPUT_MESSAGE, ComponentSupports.BINARY_OUTPUT_MESSAGE,
        ComponentSupports.TEXT_INPUT_MESSAGE, ComponentSupports.TEXT_OUTPUT_MESSAGE,
        ComponentSupports.ENTITY_INPUT_MESSAGE, ComponentSupports.ENTITY_OUTPUT_MESSAGE })
public class NoOpProcessor extends AbstractComponent {

    public static final String TYPE = "No Op";

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        messageTarget.put(inputMessage);
    }

}
