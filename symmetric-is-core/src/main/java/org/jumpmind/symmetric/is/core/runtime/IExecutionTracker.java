package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.runtime.component.IComponentRuntime;

public interface IExecutionTracker {
    
    public void flowStepStarted(IComponentRuntime component);
    
    public void beforeHandle(IComponentRuntime component);
    
    public void afterHandle(IComponentRuntime component, Throwable error);
    
    public void flowStepFinished(IComponentRuntime component, Throwable error, boolean cancelled);
    
    public void beforeFlow(String executionId);
    
    public void afterFlow();
    
    public void flowStepFailedOnComplete(IComponentRuntime component, Throwable error);

    public void log (LogLevel level, IComponentRuntime component, String output);
    
    public String getExecutionId();
    
}
