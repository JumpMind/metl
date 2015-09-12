package org.jumpmind.metl.core.runtime;

import org.jumpmind.metl.core.runtime.component.ComponentContext;

public interface IExecutionTracker {
    
    public void flowStepStarted(ComponentContext context);
    
    public void beforeHandle(ComponentContext context);
    
    public void afterHandle(ComponentContext context, Throwable error);
    
    public void updateStatistics(ComponentContext context);
    
    public void flowStepFinished(ComponentContext context, Throwable error, boolean cancelled);
    
    public void beforeFlow(String executionId);
    
    public void afterFlow();
    
    public void flowStepFailedOnComplete(ComponentContext context, Throwable error);

    public void log (LogLevel level, ComponentContext context, String output, Object... args);
    
    public String getExecutionId();
    
}
