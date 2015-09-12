package org.jumpmind.metl.core.model;

public class ComponentEntitySetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentId;
    String entityId;

    public ComponentEntitySetting() {
    }
    
    public ComponentEntitySetting(String entityId, String componentId, String name, String value) {
        super(name, value);
        this.componentId = componentId;
        this.entityId = entityId;
    }

    public ComponentEntitySetting(String entityId, String name, String value) {
        super(name, value);
        this.entityId = entityId;
    }

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
