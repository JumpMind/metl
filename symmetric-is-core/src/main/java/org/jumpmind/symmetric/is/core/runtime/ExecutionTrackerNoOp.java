package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.runtime.component.ComponentContext;

public class ExecutionTrackerNoOp implements IExecutionTracker {

    String executionId;
    
    @Override
    public void afterHandle(ComponentContext context, Throwable error) {
    }

    @Override
    public void beforeHandle(ComponentContext context) {
    }

    @Override
    public void flowStepFinished(ComponentContext context, Throwable error, boolean cancelled) {
    }

    @Override
    public void beforeFlow(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public void afterFlow() {
    }

    @Override
    public void log(LogLevel level, ComponentContext context, String output, Object...args) {
    }
    
    @Override
    public void updateStatistics(ComponentContext context) {
    }

    @Override
    public void flowStepStarted(ComponentContext context) {
    }
    
    @Override
    public void flowStepFailedOnComplete(ComponentContext context, Throwable error) {
    }
    
    @Override
    public String getExecutionId() {
        return executionId;
    }
}
