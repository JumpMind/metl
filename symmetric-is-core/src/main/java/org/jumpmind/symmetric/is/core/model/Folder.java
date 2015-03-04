package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.List;

public class Folder extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String type;

    String name;
    
    Folder parent;

    List<Folder> children;

    public Folder() {
    	children = new ArrayList<Folder>();
    }
    
    public Folder(String id) {
    	this();
        this.id = id;         
    }
    
    public FolderType getFolderType() {
        return FolderType.valueOf(type);
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
        return folder.getParentFolderId() != null
                && folder.getParentFolderId().equals(id);
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
        if (parentFolderId != null) {
            this.parent = new Folder(parentFolderId);
        } else {
            this.parent = null;
        }
    }
    
    public String getParentFolderId() {
    	return parent != null ? parent.getId() : null;
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
}
