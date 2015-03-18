package org.jumpmind.symmetric.is.core.model;

public class ComponentVersionEntitySetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentVersionId;
    String entityId;

    public void setComponentVersionId(String componentVersionId) {
        this.componentVersionId = componentVersionId;
    }

    public String getComponentVersionId() {
        return componentVersionId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
