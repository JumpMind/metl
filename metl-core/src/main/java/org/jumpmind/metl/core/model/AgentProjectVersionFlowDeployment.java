package org.jumpmind.metl.core.model;

import java.io.Serializable;

import org.jumpmind.metl.core.runtime.LogLevel;

public class AgentProjectVersionFlowDeployment implements Serializable {

    private static final long serialVersionUID = 1L;
    protected AgentDeploy agentDeployment;
    protected Flow flow;
    protected ProjectVersion projectVersion;
    
    public void setAgentDeployment(AgentDeploy agentDeployment) {
        this.agentDeployment = agentDeployment;
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    public void setProjectVersion(ProjectVersion projectVersion) {
        this.projectVersion = projectVersion;
    }

    public AgentProjectVersionFlowDeployment(AgentDeploy agentDeployment, Flow flow, ProjectVersion projectVersion) {
        this.agentDeployment = agentDeployment;
        this.flow = flow;
        this.projectVersion = projectVersion;
    }

    public AgentProjectVersionFlowDeployment() {
    }

    public AgentDeploy getAgentDeployment() {
        return agentDeployment;
    }

    public Flow getFlow() {
        return flow;
    }

    public ProjectVersion getProjectVersion() {
        return projectVersion;
    }
    
    public String getName() {
        return agentDeployment.getName();
    }
    
    public LogLevel asLogLevel() {
        return agentDeployment.asLogLevel();
    }
        

}
