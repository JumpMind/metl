package org.jumpmind.symmetric.is.core.model;


public class ComponentFlowNodeLink extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String sourceNodeId;
    
    String targetNodeId;
    
    public ComponentFlowNodeLink() {
    }
    
    public ComponentFlowNodeLink(String sourceNodeId, String targetNodeId) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }
    
    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }
    
    public String getSourceNodeId() {
        return sourceNodeId;
    }
    
    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }
    
    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setName(String name) {        
    }
    
    public String getName() {
        return sourceNodeId + " to " + targetNodeId;
    }

}
