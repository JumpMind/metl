package org.jumpmind.metl.core.model;


public class ComponentSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentId;
    
    public ComponentSetting() {
    }
    
    public ComponentSetting(String componentVersionId) {
        this.componentId = componentVersionId;
    }

    public ComponentSetting(String name, String value) {
        super(name, value);
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

}
