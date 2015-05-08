package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = Multiplier.TYPE,
        iconImage = "multiplier.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY,
        inputOutputModelsMatch=true
        )
public class Multiplier extends AbstractComponentRuntime {

    public static final String TYPE = "Multiplier";

    @SettingDefinition(
            order = 20,
            required = false,
            type = Type.INTEGER,
            defaultValue = "10",
            label = "Rows/Msg")
    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.SOURCE_STEP,
            label = "Multiplier Source")
    public final static String MULTIPLIER_SOURCE_STEP = "multiplier.source.step";

    boolean multipliersInitialized = false;

    String sourceStepId;

    int rowsPerMessage;

    List<EntityData> multipliers = new ArrayList<EntityData>();

    List<Message> queuedWhileWaitingForMultiplier = new ArrayList<Message>();

    @Override
    public void start() {
        

        multipliersInitialized = false;

        sourceStepId = getComponent().get(MULTIPLIER_SOURCE_STEP);
        rowsPerMessage = getComponent().getInt(ROWS_PER_MESSAGE, 10);

        if (isBlank(sourceStepId) || getFlow().findFlowStepWithId(sourceStepId) == null) {
            throw new IllegalStateException("The source step must be specified");
        }
    }

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();

        if (inputMessage.getHeader().getOriginatingStepId().equals(sourceStepId)) {
            List<EntityData> datas = inputMessage.getPayload();
            multipliers.addAll(datas);
            multipliersInitialized = inputMessage.getHeader().isLastMessage();
            
            Iterator<Message> messages = queuedWhileWaitingForMultiplier.iterator();
            while (messages.hasNext()) {
                Message message = messages.next();
                multiply(message, messageTarget);
            }

        } else if (!multipliersInitialized) {
            queuedWhileWaitingForMultiplier.add(inputMessage);
        } else if (multipliersInitialized) {
            multiply(inputMessage, messageTarget);
        }
    

    }

    protected void multiply(Message message, IMessageTarget messageTarget) {
        ArrayList<EntityData> multiplied = new ArrayList<EntityData>();
        for (int i = 0; i < multipliers.size(); i++) {
            EntityData multiplierData = multipliers.get(i);

            List<EntityData> datas = message.getPayload();
            for (int j = 0; j < datas.size(); j++) {
                getComponentStatistics().incrementNumberEntitiesProcessed();
                EntityData oldData = datas.get(j);
                EntityData newData = new EntityData();
                newData.putAll(oldData);
                newData.putAll(multiplierData);
                multiplied.add(newData);
                if (multiplied.size() >= rowsPerMessage) {
                    Message newMessage = new Message(getFlowStepId());
                    newMessage.getHeader().setLastMessage(
                            message.getHeader().isLastMessage() && datas.size() - 1 == j
                                    && multipliers.size() - 1 == i);
                    newMessage.setPayload(multiplied);
                    getComponentStatistics().incrementOutboundMessages();
                    messageTarget.put(newMessage);
                    multiplied = new ArrayList<EntityData>();
                }
            }
        }

        if (multiplied.size() > 0) {
            Message newMessage = new Message(getFlowStepId());
            newMessage.setPayload(multiplied);
            newMessage.getHeader().setLastMessage(message.getHeader().isLastMessage());
            getComponentStatistics().incrementOutboundMessages();
            messageTarget.put(newMessage);
        }
    }

}
