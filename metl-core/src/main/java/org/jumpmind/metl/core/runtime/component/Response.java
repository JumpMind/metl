package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Response extends AbstractComponentRuntime {

    StringBuilder response = new StringBuilder();
    
    public Response() {
    }

    @Override
    protected void start() {
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            TextMessage textMessage = (TextMessage)inputMessage;
            ArrayList<String> payload = textMessage.getPayload();
            if (payload != null) {
                for (String string : payload) {
                    response.append(string);
                }
            }
        }
    }
    
    public StringBuilder getResponse() {
        return response;
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }


}
