package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;
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

    String sequenceAttributeId;

    String sql;

    Long currentSequence;

    @Override
    protected void start() {
        Component component = getComponent();
        String sequenceAttributeName = component.get(SEQ_ATTRIBUTE);

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
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        if (currentSequence == null) {
            final String sqlToExecute = FormatUtils.replaceTokens(this.sql, context.getFlowParametersAsString(), true);
            log(LogLevel.DEBUG, "About to run: " + sqlToExecute);
            currentSequence = getJdbcTemplate()
                    .queryForObject(sqlToExecute, context.getFlowParameters(), Long.class);
        }

        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> outgoingPayload = new ArrayList<EntityData>();
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (EntityData entityData : payload) {
                entityData = entityData.copy();
                entityData.put(sequenceAttributeId, ++currentSequence);
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
