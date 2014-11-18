package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.config.ComponentVersion;

public interface IExecutionTracker {
    
    public void beforeHandle(String executionId, ComponentVersion componentVersion);
    
    public void afterHandle(String executionId, ComponentVersion componentVersion);
    
    public void beforeFlow(String executionId);
    
    public void afterFlow(String executionId);

    public void log (String executionId, LogLevel level, ComponentVersion componentVersion, String category, String output);
    
}
