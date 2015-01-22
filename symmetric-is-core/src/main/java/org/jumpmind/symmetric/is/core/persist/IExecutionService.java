package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Execution;

public interface IExecutionService {

    public Execution requestExecution(AgentDeployment deployment);
    
    public List<Execution> findActiveExecutions(Agent agent);
    
    public void saveExecutionStatus(Execution execution);
    
}
