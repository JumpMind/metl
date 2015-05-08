package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.runtime.component.ComponentContext;

public interface IExecutionTracker {
    
    public void flowStepStarted(ComponentContext component);
    
    public void beforeHandle(ComponentContext component);
    
    public void afterHandle(ComponentContext component, Throwable error);
    
    public void flowStepFinished(ComponentContext component, Throwable error, boolean cancelled);
    
    public void beforeFlow(String executionId);
    
    public void afterFlow();
    
    public void flowStepFailedOnComplete(ComponentContext component, Throwable error);

    public void log (LogLevel level, ComponentContext component, String output, Object... args);
    
    public String getExecutionId();
    
}
