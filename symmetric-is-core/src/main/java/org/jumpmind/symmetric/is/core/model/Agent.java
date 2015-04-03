package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Agent extends AbstractObjectWithSettings {

    private static final long serialVersionUID = 1L;

    Folder folder;

    String name;

    String host;

    String startMode = AgentStartMode.AUTO.name();

    String status = AgentStatus.STOPPED.name();

    Date lastStartTime;

    Date heartbeatTime;

    List<AgentDeployment> agentDeployments;

    public Agent() {
        this.agentDeployments = new ArrayList<AgentDeployment>();
    }

    public Agent(Folder folder, Setting... settings) {
        super(settings);
        setFolder(folder);
        this.agentDeployments = new ArrayList<AgentDeployment>();
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public AgentStartMode getAgentStartMode() {
        return startMode == null ? AgentStartMode.MANUAL : AgentStartMode.valueOf(startMode);
    }

    public AgentStatus getAgentStatus() {
        return status == null ? AgentStatus.UNKNOWN : AgentStatus.valueOf(status);
    }

    public void setAgentStatus(AgentStatus agentStatus) {
        status = agentStatus.name();
    }

    @Override
    protected Setting createSettingData() {
        return new AgentSetting();
    }

    public Folder getFolder() {
        return folder;
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

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setStartMode(String startMode) {
        this.startMode = startMode;
    }

    public String getStartMode() {
        return startMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(Date lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public Date getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(Date heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public List<AgentDeployment> getAgentDeployments() {
        return agentDeployments;
    }

    public void setAgentDeployments(List<AgentDeployment> agentDeployments) {
        this.agentDeployments = agentDeployments;
    }

    public boolean isDeployed(Flow flow) {
        return getAgentDeploymentFor(flow) != null;
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    public AgentDeployment getAgentDeploymentFor(Flow flow) {
        for (AgentDeployment agentDeployment : agentDeployments) {
            if (agentDeployment.getFlow().equals(flow)) {
                return agentDeployment;
            }
        }
        return null;
    }

}
