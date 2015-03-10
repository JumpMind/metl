package org.jumpmind.symmetric.is.core.persist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;

abstract public class AbstractExecutionService extends AbstractService implements IExecutionService {
    
    public AbstractExecutionService(IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
    }

    public Execution findExecution(String id) {
    	Execution e = new Execution();
    	e.setId(id);
        persistenceManager.refresh(e, null, null, tableName(e.getClass()));
        return e;
    }

    public List<ExecutionStep> findExecutionStep(String executionId) {
    	Map<String, Object> args = new HashMap<String, Object>();
    	args.put("executionId", executionId);
    	return persistenceManager.find(ExecutionStep.class, args);
    }

    public List<ExecutionStepLog> findExecutionStepLog(String executionStepId) {
    	Map<String, Object> args = new HashMap<String, Object>();
    	args.put("executionStepId", executionStepId);
    	return persistenceManager.find(ExecutionStepLog.class, args);
    }

    @Override
    public void saveExecutionStatus(Execution execution) {
    }

}
