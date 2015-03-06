package org.jumpmind.symmetric.is.core.runtime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MessageHeader implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    String executionId;

    int sequenceNumber;

    boolean lastMessage;
    
    String originatingStepId;

    Map<String, Serializable> parameters = new HashMap<String, Serializable>();
    
    public MessageHeader(String originatingStepId) {
        this.originatingStepId = originatingStepId;
    }

    public void setOriginatingStepId(String originatingStepId) {
        this.originatingStepId = originatingStepId;
    }
    
    public String getOriginatingStepId() {
        return originatingStepId;
    }
    
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String groupId) {
        this.executionId = groupId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public boolean isLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(boolean lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Serializable> parameters) {
        this.parameters = parameters;
    }

    public MessageHeader copy() {
        try {
            MessageHeader header = (MessageHeader) this.clone();
            header.parameters = new HashMap<String, Serializable>(parameters);
            return header;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

}
