package org.jumpmind.symmetric.is.core.runtime;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class MessageHeader extends HashMap<String, Serializable> implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    String executionId;

    int sequenceNumber;

    boolean lastMessage;

    String originatingStepId;

    Collection<String> targetStepIds;

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

    public void setTargetStepIds(Collection<String> targetStepIds) {
        this.targetStepIds = targetStepIds;
    }

    public Collection<String> getTargetStepIds() {
        if (targetStepIds == null) {
            targetStepIds = new HashSet<String>();
        }
        return targetStepIds;
    }

    public MessageHeader clone() {
        MessageHeader mh = new MessageHeader(originatingStepId);
        mh.putAll(this);
        mh.setExecutionId(executionId);
        mh.setSequenceNumber(sequenceNumber);
        mh.setLastMessage(lastMessage);
        if (targetStepIds != null) {
            mh.setTargetStepIds(new HashSet<String>(targetStepIds));
        }
        return mh;
    }
    
}
