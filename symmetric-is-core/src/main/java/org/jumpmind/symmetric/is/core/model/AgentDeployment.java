package org.jumpmind.symmetric.is.core.model;

public class AgentDeployment extends AbstractObject {

    private static final long serialVersionUID = 1L;

    FlowVersion flowVersion;

    String agentId;

    String startMode;

    String status = DeploymentStatus.UNKNOWN.name();

    String message;

    public AgentDeployment() {
    }

    public AgentDeployment(FlowVersion flowVersion) {
        setFlowVersion(flowVersion);
    }
    
    public void setFlowVersion(FlowVersion flowVersion) {
        this.flowVersion = flowVersion;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getFlowVersionId() {
    	return flowVersion != null ? flowVersion.getId() : null;
    }

    public void setFlowVersionId(String flowVersionId) {
        if (flowVersionId != null) {
            this.flowVersion = new FlowVersion();
            this.flowVersion.setId(flowVersionId);
        } else {
            this.flowVersion = null;
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
        return flowVersion.getFlow().getName();
    }

    public void setName(String name) {
    }

    public FlowVersion getFlowVersion() {
        return flowVersion;
    }

    @Override
    public String toString() {
        return flowVersion.getName();
    }
}
