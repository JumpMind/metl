package org.jumpmind.symmetric.is.core.config;

public class ComponentVersionAttributeSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String componentVersionId;
    String attributeId;
	public String getFormatVersionId() {
		return componentVersionId;
	}
	public void setFormatVersionId(String formatVersionId) {
		this.componentVersionId = formatVersionId;
	}
	public String getAttributeId() {
		return attributeId;
	}
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

}
