package org.jumpmind.metl.core.model;

import java.util.UUID;

public class Resource extends AbstractObjectWithSettings {

    private static final long serialVersionUID = 1L;

    Folder folder;

    String name;

    String type;

    String projectVersionId;
    
    boolean deleted = false;
    
    String rowId = UUID.randomUUID().toString();

    public Resource() {
    }

    public Resource(String id) {
        this.id = id;
    }

    public Resource(Folder folder, Setting... settings) {
        super(settings);
        setFolder(folder);
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

    public void setFolderId(String folderId) {
        if (folderId != null) {
            folder = new Folder(folderId);
        } else {
            folder = null;
        }
    }

    public String getFolderId() {
        return folder != null ? folder.getId() : null;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    protected Setting createSettingData() {
        return new ResourceSetting(id);
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
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
