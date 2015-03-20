package org.jumpmind.symmetric.is.core.model;

public class ComponentEntitySetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentId;
    String entityId;

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
