package org.jumpmind.metl.core.runtime.component;

import java.io.Serializable;

public class Results implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Object value;
    
    protected String contentType;
    
    public Results(Object value, String contentType) {
        this.value = value;
        this.contentType = contentType;
    }
    
    public Results() {
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setValue(Object result) {
        this.value = result;
    }
    
    public Object getValue() {
        return value;
    }
}
