package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.runtime.component.IComponent;

public interface IExecutionTracker {
    
    public void flowStepStarted(String executionId, IComponent component);
    
    public void beforeHandle(String executionId, IComponent component);
    
    public void afterHandle(String executionId, IComponent component, Throwable error);
    
    public void flowStepFinished(String executionId, IComponent component, Throwable error);
    
    public void beforeFlow(String executionId);
    
    public void afterFlow(String executionId);

    public void log (String executionId, LogLevel level, IComponent component, String output);
    
}
