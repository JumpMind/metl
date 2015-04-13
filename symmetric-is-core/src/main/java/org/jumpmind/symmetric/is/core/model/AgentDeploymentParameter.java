package org.jumpmind.symmetric.is.core.model;

public class AgentDeploymentParameter extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String name;
    String value;
    String agentDeploymentId;
    
    public AgentDeploymentParameter() {
    }
    
    public AgentDeploymentParameter(String name, String value, String agentDeploymentId) {
        this.name = name;
        this.value = value;
        this.agentDeploymentId = agentDeploymentId;
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
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

}
