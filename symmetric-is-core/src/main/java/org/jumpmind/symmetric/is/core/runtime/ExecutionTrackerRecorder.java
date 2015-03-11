package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.runtime.flow.AsyncRecorder;

public class ExecutionTrackerRecorder extends ExecutionTrackerLogger {

    AsyncRecorder recorder;
    
    public ExecutionTrackerRecorder(AgentDeployment deployment, AsyncRecorder recorder) {
        super(deployment);
        this.recorder = recorder;
    }

    @Override
    public void beforeFlow(String executionId) {
        super.beforeFlow(executionId);
    }

    @Override
    public void afterFlow(String executionId) {
        super.afterFlow(executionId);
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
