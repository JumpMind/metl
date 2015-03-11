package org.jumpmind.symmetric.is.core.persist;

import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;

public interface IExecutionService {

    public List<Execution> findActiveExecutions(Agent agent);
    
    public void save(AbstractObject object);

    public Execution findExecution(String id);

    public List<ExecutionStep> findExecutionStep(String executionId);

    public List<ExecutionStepLog> findExecutionStepLog(String executionStepId);
    
    public List<ExecutionStepLog> findExecutionStepLog(Set<String> executionStepIds);
    	
}
