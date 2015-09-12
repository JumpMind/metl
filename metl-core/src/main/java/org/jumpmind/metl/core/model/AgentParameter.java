package org.jumpmind.metl.core.model;


public class AgentParameter extends Setting {

    private static final long serialVersionUID = 1L;

    String agentId;
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getAgentId() {
        return agentId;
    }
}
