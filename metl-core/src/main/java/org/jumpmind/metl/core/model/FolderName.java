package org.jumpmind.metl.core.model;

public class FolderName extends AbstractName {

    private static final long serialVersionUID = 1L;
    
    String type;
    
    String parentFolderId;
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
    
    public String getParentFolderId() {
        return parentFolderId;
    }
    
}
