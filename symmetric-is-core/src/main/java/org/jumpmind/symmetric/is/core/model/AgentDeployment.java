package org.jumpmind.symmetric.is.core.model;

public class AgentDeployment extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Flow flow;    

    String agentId;

    String startMode;

    String status = DeploymentStatus.UNKNOWN.name();

    String message;

    public AgentDeployment() {
    }

    public AgentDeployment(Flow flow) {
        setFlow(flow);
    }
    
    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getFlowId() {
    	return flow != null ? flow.getId() : null;
    }

    public void setFlowId(String flowId) {
        if (flowId != null) {
            this.flow = new Flow();
            this.flow.setId(flowId);
        } else {
            this.flow = null;
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
        return flow.getName();
    }

    public void setName(String name) {
    }

    public Flow getFlow() {
        return flow;
    }

    @Override
    public String toString() {
        return flow.getName();
    }
}
