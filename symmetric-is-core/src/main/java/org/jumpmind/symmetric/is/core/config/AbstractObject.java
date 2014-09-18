package org.jumpmind.symmetric.is.core.config;

abstract public class AbstractObject<D> {

    protected D data;
    
    public AbstractObject() {
    }
    
    public AbstractObject(D data) {
        this.data = data;
    }
    
    public D getData() {
        return this.data;
    }
}
