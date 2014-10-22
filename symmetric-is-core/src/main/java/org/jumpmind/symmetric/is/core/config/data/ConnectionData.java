package org.jumpmind.symmetric.is.core.config.data;

public class ConnectionData extends AbstractData {
    
    private static final long serialVersionUID = 1L;

    String name;
    
    String type;
    
    String folderId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
    
    public String getFolderId() {
        return folderId;
    }
    
}
