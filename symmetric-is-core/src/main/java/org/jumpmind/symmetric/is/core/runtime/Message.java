package org.jumpmind.symmetric.is.core.runtime;

import java.io.Serializable;

import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class Message<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    String messageGroupId;
    
    int messageGroupSequenceNumber;
    
    boolean lastInGroup = true;
    
    LinkedCaseInsensitiveMap<Object> header;

    T payload;

    public LinkedCaseInsensitiveMap<Object> getHeader() {
        return header;
    }

    public void setHeader(LinkedCaseInsensitiveMap<Object> header) {
        this.header = header;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }    
    
}
