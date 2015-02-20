package org.jumpmind.symmetric.is.core.config;

public class FormatVersionEntitySetting extends Setting {

    private static final long serialVersionUID = 1L;

    String formatVersionId;
    String entityId;
    
	public String getFormatVersionId() {
		return formatVersionId;
	}
	public void setFormatVersionId(String formatVersionId) {
		this.formatVersionId = formatVersionId;
	}
	public String getEntityId() {
		return entityId;
	}
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
}
