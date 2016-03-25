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
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.FormatUtils;

public class EntityResult extends AbstractComponentRuntime {

    ArrayList<EntityRow> response;

    public EntityResult() {
    }

    @Override
    protected void start() {
        if (getInputModel() == null) {
            throw new IllegalStateException("An entity response component must have an input model defined");
        }
        response = new ArrayList<>();
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
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
                                    response.add(row);
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
    }

    public ArrayList<EntityRow> getResponse() {
        return response;
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
