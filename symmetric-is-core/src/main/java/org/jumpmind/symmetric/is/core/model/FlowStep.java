package org.jumpmind.symmetric.is.core.model;


public class FlowStep extends AbstractObject {

    private static final long serialVersionUID = 1L;    
    
    protected ComponentVersion componentVersion;
    
    String flowVersionId;
    
    int x;
    
    int y;
    
    public FlowStep() {
    }

    public FlowStep(ComponentVersion componentVersion) {
        this.componentVersion = componentVersion;
    }

    public ComponentVersion getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(ComponentVersion componentVersion) {
        this.componentVersion = componentVersion;
    }
    
    public void setName(String name) {
    }
    
    public String getName() {
        return this.componentVersion.getComponent().getName();
    }
    
    public String getComponentVersionId() {
        return componentVersion != null ? componentVersion.getId() : null;
    }

    public void setComponentVersionId(String componentVersionId) {
        if (componentVersionId != null) {
            this.componentVersion = new ComponentVersion(componentVersionId);
        } else {
            this.componentVersion = null;
        }
    }

    public String getFlowVersionId() {
        return flowVersionId;
    }

    public void setFlowVersionId(String flowVersionId) {
        this.flowVersionId = flowVersionId;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getX() {
        return x;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getY() {
        return y;
    }

}
