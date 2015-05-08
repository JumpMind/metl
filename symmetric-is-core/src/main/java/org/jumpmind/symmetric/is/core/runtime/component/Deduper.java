package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = Deduper.TYPE,
        iconImage = "deduper.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY,
        inputOutputModelsMatch = true)
public class Deduper extends AbstractComponent {

    public static final String TYPE = "Deduper";

    @SettingDefinition(
            order = 20,
            required = false,
            type = Type.INTEGER,
            defaultValue = "1000",
            label = "Rows/Msg")
    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    Map<String, Serializable> parameters;

    int rowsPerMessage = 1000;

    LinkedHashMap<String, EntityData> deduped = new LinkedHashMap<String, EntityData>();

    @Override
    public void start(String executionId, IExecutionTracker executionTracker) {
        super.start(executionId, executionTracker);
        rowsPerMessage = flowStep.getComponent().getInt(ROWS_PER_MESSAGE, rowsPerMessage);
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        if (parameters == null) {
            parameters = inputMessage.getHeader().getParameters();
        }
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (EntityData entityData : payload) {
                String key = entityData.toString();
                if (!deduped.containsKey(key)) {
                    componentStatistics.incrementNumberEntitiesProcessed();
                    deduped.put(key, entityData);
                }
            }
        }
    }

    @Override
    public void lastMessageReceived(IMessageTarget messageTarget) {

        if (deduped.size() > 0) {
            int count = 0;
            ArrayList<EntityData> payload = new ArrayList<EntityData>(rowsPerMessage);
            for (EntityData data : deduped.values()) {
                if (count > rowsPerMessage) {
                    sendMessage(payload, messageTarget, false);
                    payload = new ArrayList<EntityData>();
                }
                payload.add(data);
                count++;
            }

            sendMessage(payload, messageTarget, true);
        }
    }

    private void sendMessage(ArrayList<EntityData> payload, IMessageTarget messageTarget,
            boolean lastMessage) {
        Message newMessage = new Message(flowStep.getId());
        newMessage.getHeader().setParameters(parameters);
        newMessage.getHeader().setLastMessage(lastMessage);
        newMessage.setPayload(payload);
        componentStatistics.incrementOutboundMessages();
        messageTarget.put(newMessage);
    }

}
