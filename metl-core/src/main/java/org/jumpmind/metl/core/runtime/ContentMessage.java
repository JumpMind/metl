package org.jumpmind.metl.core.runtime;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

abstract public class ContentMessage<T extends Serializable> extends Message {

    private static final long serialVersionUID = 1L;
    
    T payload;

    public ContentMessage(String originatingStepId, T payload) {
        super(originatingStepId);
        this.payload = payload;
    }

    public ContentMessage(String originatingStepId) {
        super(originatingStepId);
    }
    
    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
    
    public String getTextFromPayload() {
        StringBuilder b = new StringBuilder();
        if (payload instanceof Collection) {
            Iterator<?> i = ((Collection<?>)payload).iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                b.append(obj);
                if (i.hasNext()) {
                    b.append(System.getProperty("line.separator"));
                }
            }
        } else if (payload instanceof CharSequence) {
            b.append((CharSequence)payload);
        }
        return b.toString();
    }
    

}
