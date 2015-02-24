package org.jumpmind.symmetric.is.core.config;

public class ComponentVersionEntitySetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentVersionId;
    String entityId;
    
	public String getFormatVersionId() {
		return componentVersionId;
	}
	public void setFormatVersionId(String formatVersionId) {
		this.componentVersionId = formatVersionId;
	}
	public String getEntityId() {
		return entityId;
	}
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
}
