package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ExecutionData;

public class Execution extends AbstractObject<ExecutionData> {

    private static final long serialVersionUID = 1L;

    public Execution(ExecutionStatus status, String agentDeploymentId) {
        this.data = new ExecutionData();
        this.data.setAgentDeploymentId(agentDeploymentId);
        setExecutionStatus(status);
        
    }
    
    public void setExecutionStatus(ExecutionStatus status) {
        data.setStatus(status.name());
    }

    public ExecutionStatus getExecutionStatus() {
        return ExecutionStatus.valueOf(data.getStatus());
    }
    
    public void setName(String name) {
    }
    
    public String getName() {
        return this.data.getId();
    }
}
