package org.jumpmind.symmetric.is.core.model;

import java.util.Date;

public class ExecutionStep extends AbstractObject {
	
    private static final long serialVersionUID = 1L;

    private String executionId;
    
    private String flowStepId;
    
    private String componentName;
    
    private String status;
    
    private long messagesReceived;
    
    private long messagesProduced;
    
    private long entitiesProcessed;
    
    private Date startTime;
    
    private Date endTime;
    
    @Override
    public void setName(String name) {
    }

    @Override
    public String getName() {
        return id;
    }

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public String getFlowStepId() {
		return flowStepId;
	}

	public void setFlowStepId(String flowStepId) {
		this.flowStepId = flowStepId;
	}

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	
    public ExecutionStatus getExecutionStatus() {
        return status == null ? null : ExecutionStatus.valueOf(status);
    }

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getMessagesReceived() {
		return messagesReceived;
	}

	public void setMessagesReceived(long messagesReceived) {
		this.messagesReceived = messagesReceived;
	}

	public long getMessagesProduced() {
		return messagesProduced;
	}

	public void setMessagesProduced(long messagesProduced) {
		this.messagesProduced = messagesProduced;
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
	
	public void setEntitiesProcessed(long entitiesProcessed) {
        this.entitiesProcessed = entitiesProcessed;
    }
	
	public long getEntitiesProcessed() {
        return entitiesProcessed;
    }

}
