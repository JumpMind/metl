package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;

@ComponentDefinition(
        typeName = TextReplace.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "regex_replace.png",
        inputMessage = MessageType.TEXT,
        outgoingMessage = MessageType.TEXT,
        resourceCategory = ResourceCategory.NONE)
public class TextReplace extends AbstractComponentRuntime {

    public static final String TYPE = "Text Replace";

    @SettingDefinition(order = 10, type = Type.TEXT, required=true, label = "Search For (regex)")
    public final static String SETTING_SEARCH_FOR = "search.for";
    
    @SettingDefinition(order = 10, type = Type.TEXT, label = "Replace With")
    public final static String SETTING_REPLACE_WITH = "replace.with";

    String searchFor;
    
    String replaceWith;   

    @Override
    protected void start() {
        Component component = getComponent();
        searchFor = component.get(SETTING_SEARCH_FOR);
        replaceWith = component.get(SETTING_REPLACE_WITH);
        if (isBlank(searchFor)) {
            throw new IllegalStateException("Requires a 'Search For' expression");
        }
        if (replaceWith == null) {
            replaceWith = "";
        }
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        List<String> in = inputMessage.getPayload();
        ArrayList<String> out = new ArrayList<String>();
        if (in != null) {
            for (String string : in) {
                getComponentStatistics().incrementNumberEntitiesProcessed();
                out.add(string.replaceAll(searchFor, replaceWith));                
            }            
        }
        
        Message message = new Message(getFlowStepId());
        message.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
        message.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        message.setPayload(out);
        messageTarget.put(message);
    }

}
