package org.jumpmind.symmetric.is.core.config;

import java.io.Serializable;

import org.jumpmind.symmetric.is.core.config.data.AbstractData;

abstract public class AbstractObject<D extends AbstractData> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    protected D data;
    
    public AbstractObject() {
    }
    
    public AbstractObject(D data) {
        this.data = data;
    }
    
    public D getData() {
        return this.data;
    }
    
    @Override
    public int hashCode() {
        return this.data.getId().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractObject<?>) {
            return this.data.getId().equals(((AbstractObject<?>)obj).getData().getId());
        } else {
            return super.equals(obj);
        }            
    }
}
