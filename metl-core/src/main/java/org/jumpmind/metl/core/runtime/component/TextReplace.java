package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class TextReplace extends AbstractComponentRuntime {

    public static final String TYPE = "Text Replace";

    public final static String SETTING_SEARCH_FOR = "search.for";
    
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
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        List<String> in = inputMessage.getPayload();
        ArrayList<String> out = new ArrayList<String>();
        if (in != null) {
            for (String string : in) {
                getComponentStatistics().incrementNumberEntitiesProcessed();
                out.add(string.replaceAll(searchFor, replaceWith));                
            }            
        }
        
        getComponentStatistics().incrementOutboundMessages();
        Message message = new Message(getFlowStepId());
        message.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
        message.getHeader().setUnitOfWorkLastMessage(inputMessage.getHeader().isUnitOfWorkLastMessage());
        message.setPayload(out);
        messageTarget.put(message);
    }

}
