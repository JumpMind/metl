package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = MappingProcessor.TYPE, inputMessage=MessageType.ENTITY_MESSAGE,
outgoingMessage=MessageType.ENTITY_MESSAGE)
public class MappingProcessor extends AbstractComponent {

    public static final String TYPE = "Mapping";

    public final static String ATTRIBUTE_MAPS_TO = "mapping.processor.attribute.maps.to";

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        componentStatistics.incrementOutboundMessages();
        messageTarget.put(inputMessage);
    }

}
