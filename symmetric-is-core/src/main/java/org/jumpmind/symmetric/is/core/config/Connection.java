package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class Connection extends AbstractObjectWithSettings<ConnectionData> {
    
    private static final long serialVersionUID = 1L;
    
    Folder folder;

    public Connection(Folder folder, ConnectionData data, SettingData... settings) {
        super(data, settings);
        this.folder = folder;
        data.setFolderId(folder.getData().getId());
    }
    
    @Override
    public String toString() {
        return getData().getName();
    }
    
    public Folder getFolder() {
        return folder;
    }
    
}
