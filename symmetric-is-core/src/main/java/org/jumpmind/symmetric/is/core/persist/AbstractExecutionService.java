package org.jumpmind.symmetric.is.core.persist;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStatus;
import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

abstract public class AbstractExecutionService extends AbstractService implements IExecutionService {
    
    ThreadPoolTaskScheduler purgeScheduler;
    
    Environment environment;
    
    public AbstractExecutionService(IPersistenceManager persistenceManager, String tablePrefix, Environment env) {
        super(persistenceManager, tablePrefix);
        this.environment = env;
        this.purgeScheduler = new ThreadPoolTaskScheduler();
        this.purgeScheduler.setThreadNamePrefix("execution-purge-job-");
        this.purgeScheduler.setPoolSize(1);
        this.purgeScheduler.initialize();
        this.purgeScheduler.setDaemon(true);
        this.purgeScheduler.scheduleWithFixedDelay(new PurgeExecutionHandler(), 60000*5);        
    }

    public Execution findExecution(String id) {
    	Execution e = new Execution();
    	e.setId(id);
        persistenceManager.refresh(e, null, null, tableName(e.getClass()));
        return e;
    }

    public List<ExecutionStep> findExecutionSteps(String executionId) {
    	Map<String, Object> args = new HashMap<String, Object>();
    	args.put("executionId", executionId);
    	List<ExecutionStep> steps = persistenceManager.find(ExecutionStep.class, args, null, null, tableName(ExecutionStep.class));
    	Collections.sort(steps, new Comparator<ExecutionStep>() {
    	    @Override
    	    public int compare(ExecutionStep o1, ExecutionStep o2) {
    	        return new Integer(o1.getApproximateOrder()).compareTo(new Integer(o2.getApproximateOrder()));
    	    }
        });
    	return steps;
    }

    public List<ExecutionStepLog> findExecutionStepLog(String executionStepId) {
    	Map<String, Object> args = new HashMap<String, Object>();
    	args.put("executionStepId", executionStepId);
    	return persistenceManager.find(ExecutionStepLog.class, args, null, null, tableName(ExecutionStepLog.class));
    }
    
    abstract public void purgeExecutions(String status, int retentionTimeInMs);    
    
    class PurgeExecutionHandler implements Runnable {
        @Override
        public void run() {
            ExecutionStatus[] toPurge = new ExecutionStatus[] { ExecutionStatus.CANCELLED, ExecutionStatus.DONE, ExecutionStatus.ERROR, ExecutionStatus.ABANDONED };
            for (ExecutionStatus executionStatus : toPurge) {
                String retentionTimeInMs = environment.getProperty("execution.retention.time.ms", Long.toString(1000*60*60*24*7));
                retentionTimeInMs = environment.getProperty("execution.retention.time.ms." + executionStatus.name().toLowerCase(), retentionTimeInMs);
                purgeExecutions(executionStatus.name(), Integer.parseInt(retentionTimeInMs));
            }
        }
    }

}
