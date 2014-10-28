package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.AgentData;
import org.jumpmind.symmetric.is.core.config.data.AgentSettingData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class Agent extends AbstractObjectWithSettings<AgentData> {

    private static final long serialVersionUID = 1L;

    Folder folder;
    
    public Agent(Folder folder, AgentData data, SettingData... settings) {
        super(data, settings);
        this.folder = folder;
        this.data.setFolderId(folder.getData().getId());
    }
    
    @Override
    protected SettingData createSettingData() {
        return new AgentSettingData();
    }
    
    public Folder getFolder() {
        return folder;
    }
    
    @Override
    public String toString() {
        return data.getName();
    }
    
}
