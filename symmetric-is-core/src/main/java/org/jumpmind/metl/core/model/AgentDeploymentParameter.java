package org.jumpmind.symmetric.is.core.model;

public class AgentDeploymentParameter extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String name;
    String value;
    String flowParameterId;
    String agentDeploymentId;
    
    public AgentDeploymentParameter() {
    }
    
    public AgentDeploymentParameter(String name, String value, 
            String agentDeploymentId, String flowParameterId) {
        this.name = name;
        this.value = value;
        this.agentDeploymentId = agentDeploymentId;
        this.flowParameterId = flowParameterId;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setAgentDeploymentId(String agentDeploymentId) {
        this.agentDeploymentId = agentDeploymentId;
    }
    
    public String getAgentDeploymentId() {
        return agentDeploymentId;
    }
    
    public void setFlowParameterId(String flowParameterId) {
        this.flowParameterId = flowParameterId;
    }
    
    public String getFlowParameterId() {
        return flowParameterId;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

}
