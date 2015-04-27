package org.jumpmind.symmetric.is.core.runtime;

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
    
    public Message copy (String originatingStepId) {
        return copy(originatingStepId, getPayload());
    }
    
    public Message copy (String originatingStepId, Serializable newPayload) {
        try {
            Message message = (Message)this.clone();
            message.header = header.copy();
            message.header.setOriginatingStepId(originatingStepId);
            message.setPayload(newPayload);
            return message;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
    
}
