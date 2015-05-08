package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = Transformer.TYPE,
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY,
        iconImage = "transformer.png",
        inputOutputModelsMatch = true)
public class Transformer extends AbstractComponentRuntime {

    public static final String TYPE = "Transformer";
    
    public static String TRANSFORM_EXPRESSION = "transform.expression";

    Map<String, String> transformsByAttributeId = new HashMap<String, String>();
   
    
    @Override
    public void start() {
        
        
        transformsByAttributeId.clear();
        
        List<ComponentAttributeSetting> settings = getComponent().getAttributeSettings();
        for (ComponentAttributeSetting setting : settings) {
            if (setting.getName().equals(TRANSFORM_EXPRESSION)) {
                if (isNotBlank(setting.getValue())) {
                    transformsByAttributeId.put(setting.getAttributeId(), setting.getValue());
                }
            }
        }
    }

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();        
        Model inputModel = getComponent().getInputModel();
        Message outputMessage = new Message(getFlowStepId());
        outputMessage.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        List<EntityData> inDatas = inputMessage.getPayload();
        ArrayList<EntityData> outDatas = new ArrayList<EntityData>(inDatas.size());
        outputMessage.setPayload(outDatas);
        
        for (EntityData inData : inDatas) {
            EntityData outData = new EntityData();
            outDatas.add(outData);
            Set<String> attributeIds = new HashSet<String>(inData.keySet());
            attributeIds.addAll(transformsByAttributeId.keySet());
            for (String attributeId : attributeIds) {
                String transform = transformsByAttributeId.get(attributeId);
                Object value = inData.get(attributeId);
                if (isNotBlank(transform)) {
                    ModelAttribute attribute = inputModel.getAttributeById(attributeId);
                    ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());                    
                    value = ModelAttributeScriptHelper.eval(attribute, value, entity, inData, transform);
                }
                if (value != ModelAttributeScriptHelper.REMOVE_ATTRIBUTE) {
                    outData.put(attributeId, value);
                }
            }            
            getComponentStatistics().incrementNumberEntitiesProcessed();
        }
        
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(outputMessage);
    }    

}
