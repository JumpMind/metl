package org.jumpmind.metl.core.model;


public class ResourceSetting extends Setting {

    private static final long serialVersionUID = 1L;
    
    String resourceId;

    public ResourceSetting() {
    }
    
    public ResourceSetting(String resourceId) {
        this.resourceId = resourceId;
    }
    
    public ResourceSetting(String name, String value) {
        super(name, value);
    }
    
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getResourceId() {
        return resourceId;
    }

    
}
