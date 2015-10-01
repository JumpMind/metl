package org.jumpmind.metl.core.runtime.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

class SendMessageCallback<T> implements ISendMessageCallback {

    List<T> payloadList = new ArrayList<T>();
    
    List<List<String>> targetStepIds = new ArrayList<List<String>>();
    
    boolean sentStartup = false;
    
    boolean sentShutdown = false;

    @SuppressWarnings("unchecked")
    @Override
    public void sendMessage(Serializable payload, boolean lastMessage, String... targetStepIds) {
        payloadList.add((T)payload);
        this.targetStepIds.add(Arrays.asList(targetStepIds));
    }
    
    @Override
    public void sendShutdownMessage(boolean cancel) {
        sentShutdown = true;
    }
    
    @Override
    public void sendStartupMessage() {
        sentStartup = true;
    }

    public List<T> getPayloadList() {
        return payloadList;
    }
    
    public List<List<String>> getTargetStepIds() {
        return targetStepIds;
    }
 
    public boolean isSentShutdown() {
        return sentShutdown;
    }
    
    public boolean isSentStartup() {
        return sentStartup;
    }
    
}