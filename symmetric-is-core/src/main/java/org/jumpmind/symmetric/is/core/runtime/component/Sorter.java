package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = Sorter.TYPE,
        iconImage = "sorter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY,
        inputOutputModelsMatch = true)
public class Sorter extends AbstractComponentRuntime {

    // TODO: Instead of making the sort attribute a single component level
    // setting
    // make it an attribute setting with the value being the sort order
    // to allow for n number of sort fields in any specific order.
    // Make custom UI to allow drag and drop ordering of the model fields

    public static final String TYPE = "Sorter";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.TEXT,
            label = "Sort Entity.Attribute")
    public final static String SORT_ATTRIBUTE = "sort.attribute";

    @SettingDefinition(
            order = 20,
            required = false,
            type = Type.INTEGER,
            defaultValue = "10",
            label = "Rows/Msg")
    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    int rowsPerMessage;
    String sortAttributeId;
    List<EntityData> sortedRecords = new ArrayList<EntityData>();

    @Override
    protected void start() {
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (int i = 0; i < payload.size(); i++) {
                getComponentStatistics().incrementNumberEntitiesProcessed();
                EntityData record = payload.get(i);
                sortedRecords.add(record);
            }
        }
    }

    @Override
    public void lastMessageReceived(IMessageTarget messageTarget) {
        ArrayList<EntityData> dataToSend = new ArrayList<EntityData>();
        sort();
        for (EntityData record : sortedRecords) {
            if (dataToSend.size() >= rowsPerMessage) {
                sendMessage(dataToSend, messageTarget, false);
                dataToSend = new ArrayList<EntityData>();
            }
            dataToSend.add(record);
        }
        if (dataToSend != null && dataToSend.size() > 0) {
            sendMessage(dataToSend, messageTarget, true);
        }
    }

    private void sendMessage(ArrayList<EntityData> dataToSend, IMessageTarget messageTarget,
            boolean lastMessage) {
        Message newMessage = new Message(getFlowStepId());
        newMessage.getHeader().setLastMessage(lastMessage);
        newMessage.setPayload(dataToSend);
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(newMessage);
    }

    private void applySettings() {
        TypedProperties properties = getComponent().toTypedProperties(getSettingDefinitions(false));
        String sortAttribute = properties.get(SORT_ATTRIBUTE);
        if (sortAttribute == null) {
            throw new IllegalStateException("The sort attribute must be specified.");
        }
        Model inputModel = this.getComponent().getInputModel();
        String[] joinAttributeElements = sortAttribute.split("[.]");
        if (joinAttributeElements.length != 2) {
            throw new IllegalStateException(
                    "The sort attribute must be specified as 'entity.attribute'");
        }
        sortAttributeId = inputModel.getAttributeByName(joinAttributeElements[0],
                joinAttributeElements[1]).getId();
        if (sortAttributeId == null) {
            throw new IllegalStateException(
                    "Sort attribute must be a valid 'entity.attribute' in the input model.");
        }
    }

    private void sort() {
        Collections.sort(sortedRecords, new Comparator<EntityData>() {
            @Override
            public int compare(EntityData o1, EntityData o2) {
                Object obj1 = o1.get(sortAttributeId);
                Object obj2 = o2.get(sortAttributeId);
                if ((obj1 instanceof Comparable || obj1 == null)
                        && (obj2 instanceof Comparable || obj2 == null)) {
                    return ObjectUtils.compare((Comparable<?>) obj1, (Comparable<?>) obj2);
                } else {
                    String str1 = obj1 != null ? obj1.toString() : null;
                    String str2 = obj2 != null ? obj2.toString() : null;
                    return ObjectUtils.compare(str1, str2);
                }
            }
        });
    }

}
