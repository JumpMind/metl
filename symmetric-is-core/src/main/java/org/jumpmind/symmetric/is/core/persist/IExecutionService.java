package org.jumpmind.symmetric.is.core.persist;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;

public interface IExecutionService {

    public void save(AbstractObject object);

    public List<Execution> findExecutions(Map<String, Object> params, int limit);

    public Execution findExecution(String id);

    public List<ExecutionStep> findExecutionStep(String executionId);

    public List<ExecutionStepLog> findExecutionStepLog(String executionStepId);
    
    public List<ExecutionStepLog> findExecutionStepLog(Set<String> executionStepIds);
    	
}
