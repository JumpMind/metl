package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.config.data.ConnectionSettingData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class Connection extends AbstractObjectWithSettings<ConnectionData> {

    private static final long serialVersionUID = 1L;

    Folder folder;

    public Connection(ConnectionData data) {
        this(null, data);
    }

    public Connection(Folder folder, ConnectionData data, SettingData... settings) {
        super(data, settings);
        setFolder(folder);
    }

    @Override
    public String toString() {
        return getData().getName();
    }
    
    public void setName(String name) {
        this.data.setName(name);
    }
    
    public String getName() {
        return this.data.getName();
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
        if (folder != null) {
            data.setFolderId(folder.getData().getId());
        }
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    protected SettingData createSettingData() {
        return new ConnectionSettingData(data.getId());
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
}
