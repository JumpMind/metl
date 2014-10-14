package org.jumpmind.symmetric.is.core.runtime;

import java.io.Serializable;

import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class Message implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    
    String messageGroupId;
    
    int messageGroupSequenceNumber;
    
    boolean lastInGroup = true;
    
    LinkedCaseInsensitiveMap<Object> header;

    Serializable payload;

    public LinkedCaseInsensitiveMap<Object> getHeader() {
        if (header == null) {
            header = new LinkedCaseInsensitiveMap<Object>();
        }
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
            return (Message)this.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
    
}
