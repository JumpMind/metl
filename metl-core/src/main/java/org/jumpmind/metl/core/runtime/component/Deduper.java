package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Deduper extends AbstractComponentRuntime {

    public static final String TYPE = "Deduper";

    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    int rowsPerMessage = 10000;

    LinkedHashMap<String, EntityData> deduped = new LinkedHashMap<String, EntityData>();

    @Override
    protected void start() {
        rowsPerMessage = getComponent().getInt(ROWS_PER_MESSAGE, rowsPerMessage);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (EntityData entityData : payload) {
                String key = entityData.toString();
                if (!deduped.containsKey(key)) {
                    getComponentStatistics().incrementNumberEntitiesProcessed();
                    deduped.put(key, entityData);
                }
            }
        }

        if (unitOfWorkLastMessage) {
            if (deduped.size() > 0) {
                int count = 0;
                ArrayList<EntityData> payload = new ArrayList<EntityData>(rowsPerMessage);
                for (EntityData data : deduped.values()) {
                    if (count >= rowsPerMessage) {
                        callback.sendMessage(payload, false);
                        payload = new ArrayList<EntityData>();
                        count = 0;
                    }
                    payload.add(data);
                    count++;
                }

                deduped.clear();

                callback.sendMessage(payload, true);
            }
        }
    }

}
