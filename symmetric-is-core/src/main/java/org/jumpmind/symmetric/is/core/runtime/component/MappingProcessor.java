package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, iconImage="mapping.png", 
typeName = MappingProcessor.TYPE, inputMessage=MessageType.ENTITY_MESSAGE,
outgoingMessage=MessageType.ENTITY_MESSAGE)
public class MappingProcessor extends AbstractComponent {

    public static final String TYPE = "Mapping";

    public final static String ATTRIBUTE_MAPS_TO = "mapping.processor.attribute.maps.to";

    Map<String, String> attrToAttrMap;
    
    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        attrToAttrMap = new HashMap<String, String>();
        List<ComponentAttributeSetting> attributeSettings = flowStep.getComponent().getAttributeSettings();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            if (attributeSetting.getName().equalsIgnoreCase(ATTRIBUTE_MAPS_TO)) {
                attrToAttrMap.put(attributeSetting.getAttributeId(), attributeSetting.getValue());
            }
        }
    }

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();
        if (inputRows == null) {
            return;
        }
        
        ArrayList<EntityData> outputRows = new ArrayList<EntityData>();
        Message outputMessage = new Message(flowStep.getId());
        outputMessage.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        
        for (EntityData inputRow : inputRows) {
            EntityData outputRow = new EntityData();
            outputRows.add(outputRow);
            for (Entry<String, Object> attrEntry : inputRow.entrySet()) {
                String newAttrId = attrToAttrMap.get(attrEntry.getKey());
                if (newAttrId != null) {
                    outputRow.put(newAttrId, attrEntry.getValue());
                } else {
                    outputRow.put(attrEntry.getKey(), null);
                }
            }
            
            for (ModelEntity entity : flowStep.getComponent().getOutputModel().getModelEntities()) {
                for (ModelAttribute attr: entity.getModelAttributes()) {
                    if (!outputRow.containsKey(attr.getId())) {
                        outputRow.put(attr.getId(), null);
                    }
                }
            }
        }

        componentStatistics.incrementOutboundMessages();
        outputMessage.setPayload(outputRows);
        messageTarget.put(outputMessage);
    }

}
