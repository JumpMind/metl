package org.jumpmind.symmetric.is.core.model;

import static org.apache.commons.lang.StringUtils.isBlank;

import org.jumpmind.symmetric.is.core.runtime.LogLevel;

public class AgentDeployment extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Flow flow;

    String agentId;

    String status = DeploymentStatus.UNKNOWN.name();

    String message;

    String logLevel = LogLevel.DEBUG.name();

    String startType = StartType.MANUAL.name();

    String startExpression;

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

    public void setStartExpression(String startExpression) {
        this.startExpression = startExpression;
    }

    public String getStartExpression() {
        return startExpression;
    }

    public void setStartType(String startType) {
        this.startType = startType;
    }

    public String getStartType() {
        return startType;
    }

    public StartType asStartType() {
        if (isBlank(startType)) {
            return StartType.MANUAL;
        } else {
            return StartType.valueOf(startType);
        }
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public String toString() {
        return flow.getName();
    }
}
