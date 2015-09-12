package org.jumpmind.metl.core.model;


public class FlowStepLink extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String sourceStepId;
    
    String targetStepId;
    
    public FlowStepLink() {
    }
    
    public FlowStepLink(String sourceStepId, String targetStepId) {
        this.sourceStepId = sourceStepId;
        this.targetStepId = targetStepId;
    }
    
    public void setSourceStepId(String sourceStepId) {
        this.sourceStepId = sourceStepId;
    }
    
    public String getSourceStepId() {
        return sourceStepId;
    }
    
    public void setTargetStepId(String targetStepId) {
        this.targetStepId = targetStepId;
    }
    
    public String getTargetStepId() {
        return targetStepId;
    }

    public void setName(String name) {        
    }
    
    public String getName() {
        return sourceStepId + " to " + targetStepId;
    }

}
