package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class Mapping extends AbstractComponentRuntime {

    public static final String TYPE = "Mapping";

    public final static String ATTRIBUTE_MAPS_TO = "mapping.processor.attribute.maps.to";

    public final static String SET_UNMAPPED_ATTRIBUTES_TO_NULL = "set.unmapped.attributes.to.null";

    Map<String, Set<String>> attrToAttrMap;

    boolean setUnmappedAttributesToNull;

    String unitOfWork;
    
    @Override
    protected void start() {
        
        validate();

        setUnmappedAttributesToNull = getComponent().getBoolean(
                SET_UNMAPPED_ATTRIBUTES_TO_NULL, false);
        unitOfWork = getComponent().get(UNIT_OF_WORK, UNIT_OF_WORK_FLOW);
        attrToAttrMap = new HashMap<String, Set<String>>();
        List<ComponentAttributeSetting> attributeSettings = getComponent()
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
        if (getComponent().getInputModel() == null) {
            message = message + "input model must be configured ";
        }

        if (getComponent().getOutputModel() == null) {
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
    public void handle( Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();
        if (inputRows == null) {
            return;
        }

        ArrayList<EntityData> outputRows = new ArrayList<EntityData>();
        Message outputMessage = new Message(getFlowStepId());
        outputMessage.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());

        for (EntityData inputRow : inputRows) {
            EntityData outputRow = new EntityData();            
            
            for (Entry<String, Object> attrEntry : inputRow.entrySet()) {
                Set<String> newAttrIds = attrToAttrMap.get(attrEntry.getKey());
                if (newAttrIds != null) {
                    for (String newAttrId : newAttrIds) {
                        outputRow.put(newAttrId, attrEntry.getValue());    
                    }                    
                }
            }

            if (setUnmappedAttributesToNull) {
                for (ModelEntity entity : getComponent().getOutputModel()
                        .getModelEntities()) {
                    for (ModelAttribute attr : entity.getModelAttributes()) {
                        if (!outputRow.containsKey(attr.getId())) {
                            outputRow.put(attr.getId(), null);
                        }
                    }
                }
            }
            
            if (outputRow.size() > 0) {
                outputRows.add(outputRow);
                getComponentStatistics().incrementNumberEntitiesProcessed();
            }
        }

        getComponentStatistics().incrementOutboundMessages();
        outputMessage.setPayload(outputRows);
        if (unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_INPUT_MESSAGE) ||
        		(unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_FLOW) && unitOfWorkLastMessage)) {
            outputMessage.getHeader().setUnitOfWorkLastMessage(true);
        }
        messageTarget.put(outputMessage);
    }
}
