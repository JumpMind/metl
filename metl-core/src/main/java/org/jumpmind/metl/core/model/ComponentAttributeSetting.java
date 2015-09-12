package org.jumpmind.metl.core.model;

public class ComponentAttributeSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentId;
    String attributeId;

    public ComponentAttributeSetting() {
    }
    
    public ComponentAttributeSetting(String attributeId, String componentId, String name, String value) {
        super(name, value);
        this.componentId = componentId;
        this.attributeId = attributeId;
    }

    public ComponentAttributeSetting(String attributeId, String name, String value) {
        super(name, value);
        this.attributeId = attributeId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

}
