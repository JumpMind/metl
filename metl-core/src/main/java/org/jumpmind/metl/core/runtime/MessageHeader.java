package org.jumpmind.metl.core.runtime;

import java.io.Serializable;
import java.util.HashMap;

public class MessageHeader extends HashMap<String, Serializable> implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    String executionId;

    int sequenceNumber;

    boolean unitOfWorkLastMessage;

    String originatingStepId;

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

    public boolean isUnitOfWorkLastMessage() {
        return unitOfWorkLastMessage;
    }

    public void setUnitOfWorkLastMessage(boolean lastMessage) {
        this.unitOfWorkLastMessage = lastMessage;
    }
    
}
