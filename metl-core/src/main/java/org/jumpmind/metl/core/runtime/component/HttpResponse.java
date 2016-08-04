package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.jumpmind.metl.core.model.EntityRow;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.FormatUtils;

public class HttpResponse extends AbstractComponentRuntime implements IHasResults {

    Object response;
    
    public HttpResponse() {
    }

    @Override
    protected void start() {
        if (getInputModel() == null) {
            response = new StringBuilder();
        } else {
            response = new ArrayList<>();
        }
    }
    
    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
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
        } else if (inputMessage instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) inputMessage;
            StringBuilder textResponse = (StringBuilder) response;
            textResponse.append(textMessage.getTextFromPayload());
        }
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
