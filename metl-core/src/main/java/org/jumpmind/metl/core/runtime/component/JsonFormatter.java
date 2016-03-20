package org.jumpmind.metl.core.runtime.component;

import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class JsonFormatter extends AbstractComponentRuntime {

    final static String JSON_START = "{ \"data\": [ ";
    String runWhen;

    StringBuilder buffer;

    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        runWhen = properties.get(RUN_WHEN);
        if (getInputModel() == null) {
            throw new IllegalStateException("A json formatter must have an input model defined");
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (buffer == null) {
            buffer = new StringBuilder(JSON_START);
        }
        if (inputMessage instanceof EntityDataMessage) {
            EntityDataMessage dataMessage = (EntityDataMessage) inputMessage;
            List<EntityData> datas = dataMessage.getPayload();
            for (EntityData entityData : datas) {
                Model inputModel = getInputModel();
                for (ModelEntity entity : inputModel.getModelEntities()) {
                    boolean included = false;
                    for (ModelAttribute attribute : entity.getModelAttributes()) {
                        if (entityData.containsKey(attribute.getId())) {
                            if (!included) {
                                buffer.append("\"").append(entity.getName()).append("\":[");
                                included = true;
                            }
                            buffer.append("\"").append(attribute.getName()).append("\":\"").append(entityData.get(attribute.getId()))
                                    .append("\",");
                        }
                    }
                    if (included) {
                        buffer.replace(buffer.length() - 1, buffer.length(), "],");
                    }
                }
            }
        }

        if (PER_MESSAGE.equals(runWhen) || (PER_UNIT_OF_WORK.equals(runWhen) && unitOfWorkBoundaryReached)) {
            finishAndSendBuffer(callback);
        }
    }

    protected void finishAndSendBuffer(ISendMessageCallback callback) {
        if (buffer != null) {
            buffer.replace(buffer.length() - 1, buffer.length(), "]}");
            callback.sendTextMessage(null, buffer.toString());
        }
        buffer = null;

    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
