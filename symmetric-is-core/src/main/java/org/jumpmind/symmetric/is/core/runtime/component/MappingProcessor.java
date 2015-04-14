package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        iconImage = "mapping.png",
        typeName = MappingProcessor.TYPE,
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY)
public class MappingProcessor extends AbstractComponent {

    public static final String TYPE = "Mapping";

    public final static String ATTRIBUTE_MAPS_TO = "mapping.processor.attribute.maps.to";

    @SettingDefinition(
            order = 10,
            required = false,
            type = Type.BOOLEAN,
            defaultValue = "false",
            label = "Set Unmapped Attributes To Null")
    public final static String SET_UNMAPPED_ATTRIBUTES_TO_NULL = "set.unmapped.attributes.to.null";

    Map<String, Set<String>> attrToAttrMap;

    boolean setUnmappedAttributesToNull;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);

        validate();

        setUnmappedAttributesToNull = flowStep.getComponent().getBoolean(
                SET_UNMAPPED_ATTRIBUTES_TO_NULL, false);

        attrToAttrMap = new HashMap<String, Set<String>>();
        List<ComponentAttributeSetting> attributeSettings = flowStep.getComponent()
                .getAttributeSettings();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            if (attributeSetting.getName().equalsIgnoreCase(ATTRIBUTE_MAPS_TO)) {
                Set<String> targets = attrToAttrMap.get(attributeSetting.getAttributeId());
                if (targets == null) {
                    targets = new HashSet<String>(2);
                    attrToAttrMap.put(attributeSetting.getAttributeId(), targets);
                }
                targets.add(attributeSetting.getValue());
            }
        }
    }

    protected void validate() {
        String message = "The ";
        if (flowStep.getComponent().getInputModel() == null) {
            message = message + "input model must be configured ";
        }

        if (flowStep.getComponent().getOutputModel() == null) {
            if (isNotBlank(message)) {
                message = message + " and the ";
            }
            message = message + " output model must be configured";
        }

        if (!message.equals("The ")) {
            throw new IllegalStateException(message);
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
                Set<String> newAttrIds = attrToAttrMap.get(attrEntry.getKey());
                if (newAttrIds != null) {
                    for (String newAttrId : newAttrIds) {
                        outputRow.put(newAttrId, attrEntry.getValue());    
                    }                    
                }
            }

            if (setUnmappedAttributesToNull) {
                for (ModelEntity entity : flowStep.getComponent().getOutputModel()
                        .getModelEntities()) {
                    for (ModelAttribute attr : entity.getModelAttributes()) {
                        if (!outputRow.containsKey(attr.getId())) {
                            outputRow.put(attr.getId(), null);
                        }
                    }
                }
            }
        }

        componentStatistics.incrementOutboundMessages();
        outputMessage.setPayload(outputRows);
        messageTarget.put(outputMessage);
    }

}
