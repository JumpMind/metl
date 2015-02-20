package org.jumpmind.symmetric.is.core.config;

public class FormatVersionAttributeSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String formatVersionId;
    String attributeId;
	public String getFormatVersionId() {
		return formatVersionId;
	}
	public void setFormatVersionId(String formatVersionId) {
		this.formatVersionId = formatVersionId;
	}
	public String getAttributeId() {
		return attributeId;
	}
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

}
