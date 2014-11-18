package org.jumpmind.symmetric.is.core.runtime;

import java.io.Serializable;

public class Message implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    
    MessageHeader header = new MessageHeader();

    Serializable payload;

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
    
    public Message copy () {
        try {
            Message message = (Message)this.clone();
            message.header = header.copy();
            return message;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
    
}
