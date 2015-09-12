package org.jumpmind.metl.core.model;

public class AgentResourceSetting extends ResourceSetting {

    private static final long serialVersionUID = 1L;

    String agentId;

    public AgentResourceSetting() {
    }

    public AgentResourceSetting(String resourceId, String agentId) {
        this.resourceId = resourceId;
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

}
