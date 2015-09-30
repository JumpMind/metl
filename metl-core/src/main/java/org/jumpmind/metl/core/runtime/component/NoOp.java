package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class NoOp extends AbstractComponentRuntime {

    public static final String TYPE = "No Op";

    @Override
    protected void start() {
    }
    
    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        callback.sendMessage(inputMessage.getPayload(), unitOfWorkLastMessage);
    }

}
