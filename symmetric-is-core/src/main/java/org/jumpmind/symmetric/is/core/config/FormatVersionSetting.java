package org.jumpmind.symmetric.is.core.config;

public class FormatVersionSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String formatVersionId;
    
    public FormatVersionSetting() {
    }
    
    public FormatVersionSetting(String formatVersionId) {
        this.formatVersionId = formatVersionId;
    }

    public FormatVersionSetting(String name, String value) {
        super(name, value);
    }
    
    public String getFormatVersionId() {
        return formatVersionId;
    }
    
    public void setFormatVersionId(String formatVersionId) {
        this.formatVersionId = formatVersionId;
    }

}
