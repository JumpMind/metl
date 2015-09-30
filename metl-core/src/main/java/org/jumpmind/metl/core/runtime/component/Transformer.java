package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Transformer extends AbstractComponentRuntime {

    public static final String TYPE = "Transformer";
    
    public static String TRANSFORM_EXPRESSION = "transform.expression";

    Map<String, String> transformsByAttributeId = new HashMap<String, String>();
   
    
    @Override
    protected void start() {
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
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        Model inputModel = getComponent().getInputModel();
        List<EntityData> inDatas = inputMessage.getPayload();
        ArrayList<EntityData> outDatas = new ArrayList<EntityData>(inDatas.size());
        
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
        
        callback.sendMessage(outDatas, unitOfWorkLastMessage);
    }    

}
