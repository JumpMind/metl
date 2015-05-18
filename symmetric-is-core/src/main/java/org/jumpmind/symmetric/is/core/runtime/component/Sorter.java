package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = Sorter.TYPE,
        iconImage = "sorter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY,
        inputOutputModelsMatch=true
        )
public class Sorter extends AbstractComponentRuntime {

    //TODO: Instead of making the sort attribute a single component level setting
    //      make it an attribute setting with the value being the sort order 
    //      to allow for n number of sort fields in any specific order.  
    //      Make custom UI to allow drag and drop ordering of the model fields
    
    public static final String TYPE = "Sorter";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.TEXT,
            label = "Sort Attribute")
    public final static String SORT_ATTRIBUTE = "sort.attribute";

    @SettingDefinition(
            order = 20,
            required = false,
            type = Type.INTEGER,
            defaultValue = "10",
            label = "Rows/Msg")
    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    int rowsPerMessage;
    String sortAttribute;
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
            addToSortList(payload);
        }
    }

    @Override
    public void lastMessageReceived(IMessageTarget messageTarget) {
        
        ArrayList<EntityData> dataToSend=null;
        sort();
        int nbrRecs=0;
        for (EntityData record:sortedRecords) {
            dataToSend = new ArrayList<EntityData>();
            nbrRecs++;
            dataToSend.add(record);
            if (dataToSend.size() >= rowsPerMessage) {
                sendMessage(dataToSend, messageTarget, nbrRecs==sortedRecords.size());
            }
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
        sortAttribute = properties.get(SORT_ATTRIBUTE);
        if (sortAttribute == null) {
            throw new IllegalStateException("Join attribute must be specified.");
        }
        Model inputModel = this.getComponent().getInputModel();
        String[] joinAttributeElements = sortAttribute.split("[.]");
        if (joinAttributeElements.length != 2) {
            throw new IllegalStateException("Join attribute must be specified as 'entity.attribute'");
        }
        sortAttributeId = inputModel.getAttributeByName(joinAttributeElements[0], joinAttributeElements[1]).getId();
        if (sortAttributeId == null) {
            throw new IllegalStateException("Join attribute must be a valid 'entity.attribute' in the input model.");
        }   
    }
    
    private void addToSortList(ArrayList<EntityData> records) {
        
        for (int i=0;i<records.size();i++) {
            getComponentStatistics().incrementNumberEntitiesProcessed();
            EntityData record = records.get(i);
            sortedRecords.add(record);
        }
    }
    
    private void sort() {
        //TODO: sort
    }
    
}
