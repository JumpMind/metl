package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Folder extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String type;

    String name;
    
    String projectVersionId;
    
    Folder parent;

    List<Folder> children;
    
    boolean virtualFolder = false;
    
    String rowId = UUID.randomUUID().toString();
    
    boolean deleted = false;

    public Folder() {
    	children = new ArrayList<Folder>();
    }
    
    public Folder(String id) {
    	this();
        this.id = id;         
    }
    
    public void makeVirtual() {
        this.virtualFolder = true;
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
        return !virtualFolder;
    }
    
    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
    }
    
    public String getProjectVersionId() {
        return projectVersionId;
    }
    
    public void setRowId(String rowId) {
        this.rowId = rowId;
    }
    
    public String getRowId() {
        return rowId;
    }
    
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
}
