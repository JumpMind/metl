package org.jumpmind.symmetric.is.core.runtime;

import java.util.Date;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStatus;
import org.jumpmind.symmetric.is.core.runtime.flow.AsyncRecorder;

public class ExecutionTrackerRecorder extends ExecutionTrackerLogger {

    AsyncRecorder recorder;
    
    Execution execution;
    
    public ExecutionTrackerRecorder(AgentDeployment deployment, AsyncRecorder recorder) {
        super(deployment);
        this.recorder = recorder;        
    }

    @Override
    public void beforeFlow(String executionId) {
        super.beforeFlow(executionId);
        execution = new Execution();
        execution.setId(executionId);
        execution.setStartTime(new Date());
        execution.setStatus(ExecutionStatus.RUNNING.name());
        execution.setAgentDeploymentId(deployment.getId());
        this.recorder.record(execution);
    }

    @Override
    public void afterFlow(String executionId) {
        super.afterFlow(executionId);
        execution.setEndTime(new Date());
        execution.setStatus(ExecutionStatus.DONE.name());
        execution.setLastModifyTime(new Date());
        this.recorder.record(execution);
    }

    @Override
    public void beforeHandle(String executionId, ComponentVersion componentVersion) {
        super.beforeHandle(executionId, componentVersion);
    }

    @Override
    public void afterHandle(String executionId, ComponentVersion componentVersion) {
        super.afterHandle(executionId, componentVersion);
    }

    @Override
    public void log(String executionId, LogLevel level, ComponentVersion componentVersion,
            String category, String output) {
        super.log(executionId, level, componentVersion, category, output);
    }

    
    
}
