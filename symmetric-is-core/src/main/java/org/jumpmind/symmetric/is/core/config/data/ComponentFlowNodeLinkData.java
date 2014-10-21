package org.jumpmind.symmetric.is.core.config.data;

import java.io.Serializable;

public class ComponentFlowNodeLinkData extends AbstractData implements Serializable  {

    private static final long serialVersionUID = 1L;

    String sourceNodeId;
    
    String targetNodeId;
       
    public ComponentFlowNodeLinkData() {
    }
    
    public ComponentFlowNodeLinkData(String sourceNodeId, String targetNodeId) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }
    
}
