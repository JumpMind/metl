package org.jumpmind.metl.core.model;

public class ResourceName extends AbstractName {

    private static final long serialVersionUID = 1L;
    
    String type;
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
}
