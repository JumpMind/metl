package org.jumpmind.metl.core.runtime.flow;

import java.io.Serializable;

public interface ISendMessageCallback {

    public void sendMessage(Serializable payload, boolean lastMessage, String... targetStepIds);
    
    public void sendShutdownMessage(boolean cancel);
    
    public void sendStartupMessage();
    
}
