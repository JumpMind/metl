package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.util.FormatUtils;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = SequenceGenerator.TYPE,
        iconImage = "sequence.png",
        resourceCategory = ResourceCategory.DATASOURCE,
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY,
        inputOutputModelsMatch = true)
public class SequenceGenerator extends AbstractDbComponent {

    public static final String TYPE = "Sequence";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.TEXT,
            label = "Sequence Attribute Name")
    public final static String SEQ_ATTRIBUTE = "sequence.attribute";

    @SettingDefinition(
            order = 20,
            required = true,
            type = Type.MULTILINE_TEXT,
            label = "Select Starting Sequence Sql")
    public final static String SQL = "sequence.sql";
    
    public final static String SHARED = "shared";
    
    public final static String SHARED_NAME = "shared.name";

    String sequenceAttributeId;

    String sql;

    Long nonSharedSequenceNumber;
    
    boolean shared = false;
    
    String sharedName;
    
    static final Map<String, Long> sharedSequence = new HashMap<String, Long>();

    @Override
    protected void start() {
        Component component = getComponent();
        String sequenceAttributeName = component.get(SEQ_ATTRIBUTE);

        shared = getComponent().getBoolean(SHARED, shared);
        if (shared) {
            sharedName = FormatUtils.replaceTokens(getComponent().get(SHARED_NAME), context.getFlowParametersAsString(), true);
            if (sharedName == null) {
                throw new IllegalStateException("The 'Shared Name' must be set when this sequence is shared");
            } else {
                info("This sequence is shared under the name: %s", sharedName);
            }
        }
        
        sql = getComponent().get(SQL);
        if (sql == null) {
            throw new IllegalStateException("An sql statement is required by the " + TYPE);
        }

        Model inputModel = component.getInputModel();
        if (inputModel == null) {
            throw new IllegalStateException("An input model is required by the " + TYPE);
        }

        String[] elements = sequenceAttributeName.split("[.]");
        if (elements.length != 2) {
            throw new IllegalStateException(
                    "The sequence attribute must be specified as 'entity.attribute'");
        }
        sequenceAttributeId = inputModel.getAttributeByName(elements[0], elements[1]).getId();
        if (sequenceAttributeId == null) {
            throw new IllegalStateException(
                    "The sequence attribute must be a valid 'entity.attribute' in the input model.");
        }

        synchronized (SequenceGenerator.class) {
            final String sqlToExecute = FormatUtils.replaceTokens(this.sql, context.getFlowParametersAsString(), true);
            log(LogLevel.DEBUG, "About to run: " + sqlToExecute);
            nonSharedSequenceNumber = getJdbcTemplate().queryForObject(sqlToExecute, context.getFlowParameters(), Long.class);
            if (nonSharedSequenceNumber == null) {
                nonSharedSequenceNumber = 1l;
            }

            if (shared) {
                Long currentValue = sharedSequence.get(sharedName);
                if (currentValue == null || currentValue < nonSharedSequenceNumber) {
                    log.info("'%s' is setting the shared sequence '%s' to %d", getFlowStep().getName(), sharedName, nonSharedSequenceNumber);
                    sharedSequence.put(sharedName, nonSharedSequenceNumber);
                }
            }
        }
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> outgoingPayload = new ArrayList<EntityData>();
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (EntityData entityData : payload) {
                entityData = entityData.copy();
                long sequence;
                if (shared) {
                    synchronized (SequenceGenerator.class) {
                        Long sequenceNumber = sharedSequence.get(sharedName);
                        sequence = ++sequenceNumber;
                        sharedSequence.put(sharedName, sequenceNumber);                        
                    }
                } else {
                    sequence = ++nonSharedSequenceNumber;
                }
                entityData.put(sequenceAttributeId, sequence);
                getComponentStatistics().incrementNumberEntitiesProcessed();
                outgoingPayload.add(entityData);
            }
            sendMessage(outgoingPayload, messageTarget, inputMessage.getHeader().isLastMessage());
        }
    }

    private void sendMessage(ArrayList<EntityData> payload, IMessageTarget messageTarget,
            boolean lastMessage) {
        Message newMessage = new Message(getFlowStepId());
        newMessage.getHeader().setLastMessage(lastMessage);
        newMessage.setPayload(payload);
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(newMessage);
    }

}
