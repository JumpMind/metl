package org.jumpmind.symmetric.is.core.config.data;

import java.util.Date;

public class ExecutionData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String agentDeploymentId;

    String status;

    Date startTime;

    Date endTime;

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
