package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.AgentDeploymentData;

public class AgentDeployment extends AbstractObject<AgentDeploymentData> {

    private static final long serialVersionUID = 1L;
    
    ComponentFlowVersion componentFlowVersion;
    
    public AgentDeployment() {
    }
    
    public AgentDeployment(ComponentFlowVersion componentFlowVersion, AgentDeploymentData data) {
        super(data);
        this.componentFlowVersion = componentFlowVersion;
    }
    
    public DeploymentStatus getStatus() {
        return DeploymentStatus.valueOf(data.getStatus());
    }
    
    public String getName() {
        return componentFlowVersion.getComponentFlow().getData().getName();
    }
    
    public ComponentFlowVersion getComponentFlowVersion() {
        return componentFlowVersion;
    }
    
    public String getMessage() {
        return data.getMessage();
    }

    @Override
    public String toString() {
        return componentFlowVersion.getName();
    }
}
