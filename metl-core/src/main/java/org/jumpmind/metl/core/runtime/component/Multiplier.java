package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Multiplier extends AbstractComponentRuntime {

    public static final String TYPE = "Multiplier";

    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    public final static String MULTIPLIER_SOURCE_STEP = "multiplier.source.step";

    boolean multipliersInitialized = false;

    String sourceStepId;

    int rowsPerMessage;

    List<EntityData> multipliers = new ArrayList<EntityData>();

    List<Message> queuedWhileWaitingForMultiplier = new ArrayList<Message>();

    @Override
    protected void start() {
        multipliersInitialized = false;

        sourceStepId = getComponent().get(MULTIPLIER_SOURCE_STEP);
        rowsPerMessage = getComponent().getInt(ROWS_PER_MESSAGE, 10);

        if (isBlank(sourceStepId) || getFlow().findFlowStepWithId(sourceStepId) == null) {
            throw new IllegalStateException("The source step must be specified");
        }
    }

    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();

        if (sourceStepId.equals(inputMessage.getHeader().getOriginatingStepId())) {
            List<EntityData> datas = inputMessage.getPayload();
            multipliers.addAll(datas);
            multipliersInitialized = inputMessage.getHeader().isUnitOfWorkLastMessage();
            
            if (multipliersInitialized) {
                Iterator<Message> messages = queuedWhileWaitingForMultiplier.iterator();
                while (messages.hasNext()) {
                    Message message = messages.next();
                    multiply(message, callback, message.getHeader().isUnitOfWorkLastMessage());
                }
            }
        } else if (!multipliersInitialized) {
            inputMessage.getHeader().setUnitOfWorkLastMessage(unitOfWorkLastMessage);
            queuedWhileWaitingForMultiplier.add(inputMessage);
        } else if (multipliersInitialized) {
            multiply(inputMessage, callback, unitOfWorkLastMessage);
        }
    }

    protected void multiply(Message message, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
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
                    callback.sendMessage(multiplied, false);                    
                    multiplied = new ArrayList<EntityData>();
                }
            }
        }

        if (multiplied.size() > 0) {
            callback.sendMessage(multiplied, true);               
        }
    }

}
