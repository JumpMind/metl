package org.jumpmind.metl.core.runtime;

import java.io.Serializable;

public class Message implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    
    MessageHeader header;

    Serializable payload;
    
    public Message(String originatingStepId) {
        this.header = new MessageHeader(originatingStepId);
    }

    public MessageHeader getHeader() {
        return header;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getPayload() {
        return (T)payload;
    }

    public <T extends Serializable> void setPayload(T payload) {
        this.payload = payload;
    }
    
    public Message clone(String originatingStepId, boolean unitOfWorkLastMessage) {
        return clone(originatingStepId, getPayload(), unitOfWorkLastMessage);
    }
    
    public Message clone(String originatingStepId, Serializable newPayload, boolean unitOfWorkLastMessage) {
        Message message = new Message(originatingStepId);
        message.header = header.clone(unitOfWorkLastMessage);
        message.header.setOriginatingStepId(originatingStepId);
        message.setPayload(newPayload);
        return message;
    }
    
}
