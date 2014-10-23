package org.jumpmind.symmetric.is.core.config.data;

public class ComponentVersionSettingData extends SettingData {

    private static final long serialVersionUID = 1L;

    String componentVersionId;
    
    public ComponentVersionSettingData() {
    }
    
    public ComponentVersionSettingData(String componentVersionId) {
        this.componentVersionId = componentVersionId;
    }

    public ComponentVersionSettingData(String name, String value) {
        super(name, value);
    }
    
    public String getComponentVersionId() {
        return componentVersionId;
    }
    
    public void setComponentVersionId(String componentVersionId) {
        this.componentVersionId = componentVersionId;
    }

}
