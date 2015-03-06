package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Execution;

public interface IExecutionService {

    public Execution requestExecution(AgentDeployment deployment);
    
    public List<Execution> findActiveExecutions(Agent agent);
    
    public void saveExecutionStatus(Execution execution);
    
}
