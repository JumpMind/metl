package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public class Folder extends AbstractObject<FolderData> {

    Folder parent;
    
    List<Folder> children = new ArrayList<Folder>();
    
    public Folder(FolderData data) {
        super(data);
    }
    
    public FolderType getFolderType() {
        return FolderType.valueOf(getData().getType());
    }
    
    public List<Folder> getChildren() {
        return children;
    }
    
    public void setParent(Folder parent) {
        this.parent = parent;
    }
    
    public Folder getParent() {
        return parent;
    }
    
    public boolean isParentOf(Folder folder) {
        return folder.getData().getParentFolderId() != null &&
                folder.getData().getParentFolderId().equals(getData().getId());        
    }
}
