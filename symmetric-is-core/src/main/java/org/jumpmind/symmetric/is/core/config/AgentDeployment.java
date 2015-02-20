package org.jumpmind.symmetric.is.core.config;

public class AgentDeployment extends AbstractObject {

    private static final long serialVersionUID = 1L;

    ComponentFlowVersion componentFlowVersion;

    String agentId;

    String startMode;

    String status = DeploymentStatus.UNKNOWN.name();

    String message;

    public AgentDeployment() {
    }

    public AgentDeployment(ComponentFlowVersion componentFlowVersion) {
        setComponentFlowVersion(componentFlowVersion);
    }
    
    public void setComponentFlowVersion(ComponentFlowVersion componentFlowVersion) {
        this.componentFlowVersion = componentFlowVersion;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getComponentFlowVersionId() {
    	return componentFlowVersion != null ? componentFlowVersion.getId() : null;
    }

    public void setComponentFlowVersionId(String componentFlowVersionId) {
        if (componentFlowVersionId != null) {
            this.componentFlowVersion = new ComponentFlowVersion();
        } else {
            this.componentFlowVersion = null;
        }
    }

    public String getStartMode() {
        return startMode;
    }

    public void setStartMode(String startMode) {
        this.startMode = startMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String errorMessage) {
        this.message = errorMessage;
    }

    public String getMessage() {
        return message;
    }

    public DeploymentStatus getDeploymentStatus() {
        return DeploymentStatus.valueOf(status);
    }

    public String getName() {
        return componentFlowVersion.getComponentFlow().getName();
    }

    public void setName(String name) {
    }

    public ComponentFlowVersion getComponentFlowVersion() {
        return componentFlowVersion;
    }

    @Override
    public String toString() {
        return componentFlowVersion.getName();
    }
}
