package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.runtime.component.IComponentRuntime;

public interface IExecutionTracker {
    
    public void flowStepStarted(String executionId, IComponentRuntime component);
    
    public void beforeHandle(String executionId, IComponentRuntime component);
    
    public void afterHandle(String executionId, IComponentRuntime component, Throwable error);
    
    public void flowStepFinished(String executionId, IComponentRuntime component, Throwable error, boolean cancelled);
    
    public void beforeFlow(String executionId);
    
    public void afterFlow(String executionId);
    
    public void flowStepFailedOnComplete(String executionId, IComponentRuntime component, Throwable error);

    public void log (String executionId, LogLevel level, IComponentRuntime component, String output);
    
}
