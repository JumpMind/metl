package org.jumpmind.metl.core.model;

public class AgentResource extends Resource {

    private static final long serialVersionUID = 1L;

    String agentId;

    public AgentResource() {
    }

    @Override
    protected Setting createSettingData() {
        return new AgentResourceSetting(id, agentId);
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

}
