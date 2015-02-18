package org.jumpmind.symmetric.is.core.config;


public class AgentSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String agentId;
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getAgentId() {
        return agentId;
    }
}
