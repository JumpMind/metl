package org.jumpmind.symmetric.is.core.model;


public class ConnectionSetting extends Setting {

    private static final long serialVersionUID = 1L;
    
    String connectionId;

    public ConnectionSetting() {
    }
    
    public ConnectionSetting(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public ConnectionSetting(String name, String value) {
        super(name, value);
    }
    
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getConnectionId() {
        return connectionId;
    }

    
}
