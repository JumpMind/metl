package org.jumpmind.metl.core.model;

import java.util.UUID;

public abstract class AbstractModel extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;
    
    String rowId = UUID.randomUUID().toString();

    String name;

    Folder folder;

    String folderId;

    String projectVersionId;
    
    boolean shared;

    boolean deleted = false;

    public AbstractModel() {        
    }
    
    public AbstractModel(String id) {
        this.setId(id);
    }

    public AbstractModel(Folder folder) {
        this.folder = folder;
        this.folderId = folder.getId();
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
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

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

}
