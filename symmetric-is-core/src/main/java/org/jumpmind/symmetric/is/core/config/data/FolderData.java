package org.jumpmind.symmetric.is.core.config.data;

public class FolderData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String parentFolderId;

    String type;

    String name;
    
    public FolderData(String id) {
        this.id = id;
    }
    
    public FolderData() {
    }

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
    
    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
    
    public String getParentFolderId() {
        return parentFolderId;
    }

}
