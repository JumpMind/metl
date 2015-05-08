package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.runtime.component.IComponentRuntime;

public class ExecutionTrackerNoOp implements IExecutionTracker {

    String executionId;
    
    @Override
    public void afterHandle(IComponentRuntime component, Throwable error) {
    }

    @Override
    public void beforeHandle(IComponentRuntime component) {
    }

    @Override
    public void flowStepFinished(IComponentRuntime component, Throwable error, boolean cancelled) {
    }

    @Override
    public void beforeFlow(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public void afterFlow() {
    }

    @Override
    public void log(LogLevel level, IComponentRuntime component, String output) {
    }

    @Override
    public void flowStepStarted(IComponentRuntime component) {
    }
    
    @Override
    public void flowStepFailedOnComplete(IComponentRuntime component, Throwable error) {
    }
    
    @Override
    public String getExecutionId() {
        return executionId;
    }
}
