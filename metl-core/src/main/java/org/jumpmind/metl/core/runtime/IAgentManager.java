package org.jumpmind.metl.core.runtime;

import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.Flow;

public interface IAgentManager {

    public AgentRuntime refresh(Agent agent);
    
    public boolean cancel(String executionId);
    
    public void remove(Agent agent);
    
    public AgentRuntime getAgentRuntime(Agent agent);
    
    public AgentRuntime getAgentRuntime(String agentId);
    
    public boolean isAgentLocal(Agent agent);
    
    public void undeploy(AgentDeployment deployment);
    
    public AgentDeployment deploy(String agentId, Flow flow, Map<String, String> parameters);
    
    public void start();
    
    public Set<Agent> getAvailableAgents();
    
    
}
