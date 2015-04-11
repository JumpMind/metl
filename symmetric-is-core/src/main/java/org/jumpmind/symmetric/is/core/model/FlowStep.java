package org.jumpmind.symmetric.is.core.model;


public class FlowStep extends AbstractObject {

    private static final long serialVersionUID = 1L;    
    
    protected Component component;
    
    String flowId;
    
    int x;
    
    int y;
    
    public FlowStep() {
    }

    public FlowStep(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }
    
    public void setName(String name) {
        this.component.setName(name);
    }
    
    public String getName() {
        return this.component.getName();
    }
    
    public String getComponentId() {
        return component != null ? component.getId() : null;
    }

    public void setComponentId(String componentId) {
        if (componentId != null) {
            this.component = new Component(componentId);
        } else {
            this.component = null;
        }
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
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

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
}
