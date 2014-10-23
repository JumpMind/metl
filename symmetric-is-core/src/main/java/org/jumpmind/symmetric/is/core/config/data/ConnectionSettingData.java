package org.jumpmind.symmetric.is.core.config.data;

public class ConnectionSettingData extends SettingData {

    private static final long serialVersionUID = 1L;
    
    String connectionId;

    public ConnectionSettingData() {
    }
    
    public ConnectionSettingData(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public ConnectionSettingData(String name, String value) {
        super(name, value);
    }
    
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getConnectionId() {
        return connectionId;
    }

    
}
