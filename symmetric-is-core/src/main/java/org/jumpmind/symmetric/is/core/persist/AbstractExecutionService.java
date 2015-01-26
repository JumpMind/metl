package org.jumpmind.symmetric.is.core.persist;

import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Execution;

abstract public class AbstractExecutionService implements IExecutionService {

    IPersistenceManager persistenceManager;

    AbstractExecutionService(IPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }
    
    @Override
    public Execution requestExecution(AgentDeployment deployment) {
        return null;
    }
    
    @Override
    public void saveExecutionStatus(Execution execution) {
    }

}
