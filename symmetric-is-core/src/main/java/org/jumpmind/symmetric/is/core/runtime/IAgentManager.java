package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.config.Agent;

public interface IAgentManager {

    public AgentEngine refresh(Agent agent);
    
    public void remove(Agent agent);
    
    public AgentEngine getAgentEngine(Agent agent);
    
    public AgentEngine getAgentEngine(String agentId);
    
    public boolean isAgentLocal(Agent agent);
    
    public void start();
    
    
}
