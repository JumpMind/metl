package org.jumpmind.symmetric.is.core.model;

import java.util.Date;

public class Execution extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String agentId;
    
    String flowId;
    
    String deploymentId;
    
    String deploymentName;
    
    String agentName;
    
    String hostName;
    
    String flowName;

    String status;

    Date startTime;

    Date endTime;

    public Execution() {
    }

    public Execution(ExecutionStatus status) {
        setExecutionStatus(status);
    }

    public void setExecutionStatus(ExecutionStatus status) {
        setStatus(status.name());
    }

    public ExecutionStatus getExecutionStatus() {
        return status == null ? null : ExecutionStatus.valueOf(status);
    }

    public void setName(String name) {
    }

    public String getName() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getFlowId() {
		return flowId;
	}

	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }
	
	public String getDeploymentId() {
        return deploymentId;
    }
	
	public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }
	
	public String getDeploymentName() {
        return deploymentName;
    }

}
