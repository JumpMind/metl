package org.jumpmind.symmetric.is.core.model;

import org.jumpmind.symmetric.is.core.runtime.LogLevel;

public class AgentDeploymentSummary extends AbstractObject {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_FLOW = "Flow";
    
    public static final String TYPE_RESOURCE = "Resource";
    
    String projectName;
    
    String type;
    
    String name;

    String status = DeploymentStatus.UNKNOWN.name();

    String logLevel = LogLevel.DEBUG.name();

    String startType = StartType.MANUAL.name();

    String startExpression;

    public AgentDeploymentSummary() {
    }

    public boolean isFlow() {
        return type.equals(TYPE_FLOW);
    }
    
    public boolean isResource() {
        return type.equals(TYPE_RESOURCE);
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getStartType() {
        return startType;
    }

    public void setStartType(String startType) {
        this.startType = startType;
    }

    public String getStartExpression() {
        return startExpression;
    }

    public void setStartExpression(String startExpression) {
        this.startExpression = startExpression;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
}
