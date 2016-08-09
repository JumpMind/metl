package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jumpmind.metl.core.model.EntityRow;
import org.jumpmind.metl.core.model.EntityTable;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.FormatUtils;

public class HttpResponse extends AbstractHttpRequestResponse implements IHasResults {
    
    Object response;
    
    public HttpResponse() {
    }

    @Override
    protected void start() {
        init();
        if (getInputModel() == null) {
            response = new StringBuilder();
        } else {
            response = new ArrayList<>();
        }
    }
    
    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
            if (getInputModel() == null) {
                throw new MisconfiguredException("If an Http Response component receives an entity message type, it must have an input model specified.");
            }
            if (payloadFormat.equals(PayloadFormat.BY_INBOUND_ROW.name())) {
                createByInboundRowPayload(inputMessage);
            } else if (payloadFormat.equals(PayloadFormat.BY_TABLE.name())) {
                createByTablePayload(inputMessage);
            }            
        } else if (inputMessage instanceof TextMessage) {
            if (getInputModel() != null) {
                throw new MisconfiguredException("If an Http Response component receives a text message, it must NOT have an input model specified");
            }
            TextMessage textMessage = (TextMessage) inputMessage;
            StringBuilder textResponse = (StringBuilder) response;
            textResponse.append(textMessage.getTextFromPayload());
        }
    }
    
    private void createByInboundRowPayload(Message inputMessage) {
        @SuppressWarnings("unchecked")
        ArrayList<EntityRow> entityResponse = (ArrayList<EntityRow>) response;
        EntityDataMessage entityMessage = (EntityDataMessage) inputMessage;
        ArrayList<EntityData> payload = entityMessage.getPayload();
        if (payload != null) {
            Model inputModel = getInputModel();
            for (EntityData entityData : payload) {
                for (ModelEntity entity : inputModel.getModelEntities()) {
                    EntityRow row = null;
                    for (ModelAttribute attribute : entity.getModelAttributes()) {
                        if (entityData.containsKey(attribute.getId())) {
                            if (row == null) {
                                row = new EntityRow(entity.getName(), new HashMap<>(entity.getModelAttributes().size()));
                                entityResponse.add(row);
                            }
                            String stringValue = null;
                            Object value = entityData.get(attribute.getId());
                            if (value instanceof Date) {
                                stringValue = FormatUtils.TIMESTAMP_FORMATTER.format((Date)value);
                            } if (value != null) {
                                stringValue = value.toString();
                            }
                            row.getData().put(attribute.getName(), stringValue);
                        }
                    }
                }
            }
        }
    }
    
    private void createByTablePayload(Message inputMessage) {

        @SuppressWarnings("unchecked")
        ArrayList<EntityTable> entityResponse = (ArrayList<EntityTable>) response;
        Map<String, EntityTable> entityTables = new HashMap<String, EntityTable>();
        
        EntityDataMessage entityMessage = (EntityDataMessage) inputMessage;        
        ArrayList<EntityData> payload = entityMessage.getPayload();
        if (payload != null) {
            Model inputModel = getInputModel();
            for (EntityData entityData : payload) {
                Iterator<String> itr = entityData.keySet().iterator();
                boolean firstAttribute = true;
                ModelEntity entity = null;
                Map<String, String> row = new HashMap<String, String>();
                while (itr.hasNext()) {
                    String attributeId = itr.next();
                    if (firstAttribute) {
                        entity = inputModel.getEntityById(inputModel.getAttributeById(attributeId).getEntityId());
                        if (!entityTables.containsKey(entity.getName())) {
                            entityTables.put(entity.getName(),new EntityTable(entity.getName()));
                        }
                        firstAttribute=false;
                    }
                    
                    String stringValue = null;
                    Object value = entityData.get(attributeId);
                    if (value instanceof Date) {
                        stringValue = FormatUtils.TIMESTAMP_FORMATTER.format((Date)value);
                    } if (value != null) {
                        stringValue = value.toString();
                    }                        
                    row.put(inputModel.getAttributeById(attributeId).getName(), stringValue);
                }                
                entityTables.get(entity.getName()).getRows().add(row);  
            }
        }        
        entityResponse.addAll(entityTables.values());
    }
    
    @Override
    public Results getResults() {
        return new Results(getResponse(), getContentType());
    }
    
    private String getContentType() {
        if (response instanceof CharSequence) {
            // content type only means anything if we are providing the output in string format
            return properties.get("content.type");
        } else {
            return null;
        }
    }

    private Object getResponse() {
        if (response instanceof CharSequence) {
            return response.toString();
        } else {
            return response;
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
