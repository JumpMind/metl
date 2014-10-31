package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.AgentData;
import org.jumpmind.symmetric.is.core.config.data.AgentSettingData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class Agent extends AbstractObjectWithSettings<AgentData> {

    private static final long serialVersionUID = 1L;

    Folder folder;
    
    List<AgentDeployment> agentDeployments;
    
    public Agent(Folder folder, AgentData data, SettingData... settings) {
        super(data, settings);
        this.folder = folder;
        this.data.setFolderId(folder.getData().getId());
        this.agentDeployments = new ArrayList<AgentDeployment>();
    }
    
    public AgentStartMode getAgentStartMode() {
        return data.getStartMode() == null ? AgentStartMode.MANUAL : AgentStartMode.valueOf(data.getStartMode());
    }
    
    public AgentStatus getAgentStatus() {
        return data.getStatus() == null ? AgentStatus.UNKNOWN : AgentStatus.valueOf(data.getStatus());
    }
    
    public void setAgentStatus(AgentStatus status) {
        data.setStatus(status.name());
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
    
    public List<AgentDeployment> getAgentDeployments() {
        return agentDeployments;
    }
    
    public void setAgentDeployments(List<AgentDeployment> agentDeployments) {
        this.agentDeployments = agentDeployments;
    }
    
}
