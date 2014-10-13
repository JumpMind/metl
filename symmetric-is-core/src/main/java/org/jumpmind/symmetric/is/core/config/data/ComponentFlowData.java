package org.jumpmind.symmetric.is.core.config.data;


public class ComponentFlowData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String name;
    
    String folderId;

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
