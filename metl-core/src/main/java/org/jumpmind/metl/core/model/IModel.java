package org.jumpmind.metl.core.model;

public interface IModel {

    public boolean isShared();

    public void setShared(boolean shared);

    public Folder getFolder();

    public void setFolder(Folder folder);
    
    public String getName();

    public void setName(String name);

    public String getFolderId();

    public void setFolderId(String folderId);

    public void setProjectVersionId(String projectVersionId);

    public String getProjectVersionId();
    
    public void setRowId(String rowId);
    
    public String getRowId();

    public boolean isSettingNameAllowed();
    
    public void setDeleted(boolean deleted);
    
    public boolean isDeleted();
    
    public String getId();
    
    public String getType();

}
