package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class Joiner extends AbstractComponentRuntime {

    public static final String TYPE = "Joiner";

    public final static String JOIN_ATTRIBUTE = "join.attribute";

    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    int rowsPerMessage;
    Map<Object, EntityData> joinedData = new LinkedHashMap<Object, EntityData>();
    
    List<String> attributesToJoinOn = new ArrayList<String>();

    @Override
    protected void start() {        
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> payload = inputMessage.getPayload();
            join(payload);
        }

        if (unitOfWorkLastMessage) {
            ArrayList<EntityData> dataToSend = new ArrayList<EntityData>();
            Iterator<EntityData> itr = joinedData.values().iterator();
            while (itr.hasNext()) {
                if (dataToSend.size() >= rowsPerMessage) {
                    sendMessage(dataToSend, messageTarget, false);
                    dataToSend = new ArrayList<EntityData>();
                }
                dataToSend.add(itr.next());
            }
            if (dataToSend != null && dataToSend.size() > 0) {
                sendMessage(dataToSend, messageTarget, true);
            }
        }
    }
    
    private void applySettings() {
        Component component = getComponent();
        Model inputModel = component.getInputModel();
        if (inputModel == null) {
            throw new IllegalStateException("The input model is required and has not yet been set");
        }
        

            List<ComponentAttributeSetting> settings = component.getAttributeSettings();
            for (ComponentAttributeSetting componentAttributeSetting : settings) {
                if (componentAttributeSetting.getName().equals(JOIN_ATTRIBUTE) && Boolean.parseBoolean(componentAttributeSetting.getValue())) {
                    attributesToJoinOn.add(componentAttributeSetting.getAttributeId());
                }
            }
    }
    
    private void join(ArrayList<EntityData> records) {        
        for (EntityData entityData : records) {
            getComponentStatistics().incrementNumberEntitiesProcessed();
            StringBuilder key = new StringBuilder();
            for (String attributeId : attributesToJoinOn) {
                if (key.length() > 0) {
                    key.append("&");
                } 
                key.append(attributeId);
                key.append("=");
                key.append(entityData.get(attributeId));
            }
            Object keyValue = key.toString();
            EntityData existingRecord = joinedData.get(keyValue);
            if (existingRecord != null) {
                mergeRecords(entityData, existingRecord);
            } else {
                joinedData.put(keyValue, entityData.copy());
            }
        }
    }
    
    private void mergeRecords(EntityData sourceRecord, EntityData targetRecord) {
        Iterator<Map.Entry<String, Object>> itr = sourceRecord.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String,Object> column = (Map.Entry<String, Object>)itr.next();
            if (column != null) {
                if (column.getValue() != null) {
                    targetRecord.put(column.getKey(), column.getValue());
                }
            }
        }
    }
}
