package org.jumpmind.symmetric.is.core.config;

public class Connection extends AbstractObjectWithSettings {

    private static final long serialVersionUID = 1L;

    Folder folder;

    String name;

    String type;

    String folderId;

    public Connection() {
    }

    public Connection(String id) {
    	this.id = id;
    }
    
    public Connection(Folder folder, Setting... settings) {
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
        this.folderId = folderId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
        if (folder != null) {
            setFolderId(folder.getId());
        } else {
            folderId = null;
        }
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    protected Setting createSettingData() {
        return new ConnectionSetting(id);
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
}
