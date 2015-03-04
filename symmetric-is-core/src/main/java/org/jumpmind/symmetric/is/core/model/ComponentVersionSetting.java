package org.jumpmind.symmetric.is.core.model;


public class ComponentVersionSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentVersionId;
    
    public ComponentVersionSetting() {
    }
    
    public ComponentVersionSetting(String componentVersionId) {
        this.componentVersionId = componentVersionId;
    }

    public ComponentVersionSetting(String name, String value) {
        super(name, value);
    }
    
    public String getComponentVersionId() {
        return componentVersionId;
    }
    
    public void setComponentVersionId(String componentVersionId) {
        this.componentVersionId = componentVersionId;
    }

}
