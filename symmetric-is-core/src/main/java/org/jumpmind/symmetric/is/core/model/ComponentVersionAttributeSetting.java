package org.jumpmind.symmetric.is.core.model;

public class ComponentVersionAttributeSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentVersionId;
    String attributeId;

    public ComponentVersionAttributeSetting() {
    }

    public ComponentVersionAttributeSetting(String attributeId, String name, String value) {
        super(name, value);
        this.attributeId = attributeId;
    }

    public void setComponentVersionId(String componentVersionId) {
        this.componentVersionId = componentVersionId;
    }

    public String getComponentVersionId() {
        return componentVersionId;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

}
