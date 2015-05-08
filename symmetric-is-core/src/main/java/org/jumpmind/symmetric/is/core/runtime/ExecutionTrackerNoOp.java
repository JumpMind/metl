package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.runtime.component.IComponentRuntime;

public class ExecutionTrackerNoOp implements IExecutionTracker {

    @Override
    public void afterHandle(String executionId, IComponentRuntime component, Throwable error) {
    }

    @Override
    public void beforeHandle(String executionId, IComponentRuntime component) {
    }

    @Override
    public void flowStepFinished(String executionId, IComponentRuntime component, Throwable error,
            boolean cancelled) {
    }

    @Override
    public void beforeFlow(String executionId) {
    }

    @Override
    public void afterFlow(String executionId) {
    }

    @Override
    public void log(String executionId, LogLevel level, IComponentRuntime component, String output) {
    }

    @Override
    public void flowStepStarted(String executionId, IComponentRuntime component) {
    }
    
    @Override
    public void flowStepFailedOnComplete(String executionId, IComponentRuntime component, Throwable error) {
    }
}
