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
    
    public String getName() {
        return componentFlowVersion.getComponentFlow().getData().getName();
    }
    
    public ComponentFlowVersion getComponentFlowVersion() {
        return componentFlowVersion;
    }

}
