package org.jumpmind.symmetric.is.core.model;

import java.util.Date;

public class Execution extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String agentDeploymentId;

    String status;

    Date startTime;

    Date endTime;

    public Execution() {
    }

    public Execution(ExecutionStatus status, String agentDeploymentId) {
        this.agentDeploymentId = agentDeploymentId;
        setExecutionStatus(status);
    }

    public void setExecutionStatus(ExecutionStatus status) {
        setStatus(status.name());
    }

    public ExecutionStatus getExecutionStatus() {
        return ExecutionStatus.valueOf(status);
    }

    public void setName(String name) {
    }

    public String getName() {
        return id;
    }

    public String getAgentDeploymentId() {
        return agentDeploymentId;
    }

    public void setAgentDeploymentId(String agentDeploymentId) {
        this.agentDeploymentId = agentDeploymentId;
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

}
