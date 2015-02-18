package org.jumpmind.symmetric.is.core.config;


public class ComponentFlowNode extends AbstractObject {

    private static final long serialVersionUID = 1L;    
    
    protected ComponentVersion componentVersion;
    
    String componentVersionId;
    
    String componentFlowVersionId;
    
    int x;
    
    int y;
    
    public ComponentFlowNode() {
    }

    public ComponentFlowNode(ComponentVersion componentVersion) {
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
        return componentVersionId;
    }

    public void setComponentVersionId(String componentVersionId) {
        this.componentVersionId = componentVersionId;
    }

    public String getComponentFlowVersionId() {
        return componentFlowVersionId;
    }

    public void setComponentFlowVersionId(String componentFlowVersionId) {
        this.componentFlowVersionId = componentFlowVersionId;
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
