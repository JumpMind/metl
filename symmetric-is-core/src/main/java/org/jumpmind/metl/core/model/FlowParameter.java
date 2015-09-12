package org.jumpmind.symmetric.is.core.model;

public class FlowParameter extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String name;
    String defaultValue;
    String flowId;
    int position;
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }
    
    public String getFlowId() {
        return flowId;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public int getPosition() {
        return position;
    }

}
