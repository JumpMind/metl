package org.jumpmind.symmetric.is.ui.api;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "executionresults")
public class ExecutionResults implements Serializable {

    private static final long serialVersionUID = 1L;

    String executionId;

    String status;

    Date startTime;

    Date endTime;
    
    String message;

    public ExecutionResults() {
    }

    public ExecutionResults(String executionId, String status, Date startTime, Date endTime) {
        super();
        this.executionId = executionId;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }

}
