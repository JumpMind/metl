package org.jumpmind.symmetric.is.core.config.data;


public class ComponentFlowNodeData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String componentVersionId;
    
    String componentFlowVersionId;
    
    int x;
    
    int y;
    
    public ComponentFlowNodeData() {
    }
    
    public ComponentFlowNodeData(String componentVersionId, String componentFlowVersionId) {
        this.componentVersionId = componentVersionId;
        this.componentFlowVersionId = componentFlowVersionId;
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
