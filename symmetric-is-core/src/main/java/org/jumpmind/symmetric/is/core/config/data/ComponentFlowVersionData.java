package org.jumpmind.symmetric.is.core.config.data;

import org.jumpmind.symmetric.is.core.config.StartType;

public class ComponentFlowVersionData extends AbstractVersionData {

    private static final long serialVersionUID = 1L;

    String componentFlowId;
    
    String startType = StartType.MANUAL.name();
    
    String startExpression;
    
    public ComponentFlowVersionData(String id) {
        this.id = id;
    }
    
    public ComponentFlowVersionData() {
    }

    public void setComponentFlowId(String componentFlowId) {
        this.componentFlowId = componentFlowId;
    }

    public String getComponentFlowId() {
        return componentFlowId;
    }

    public void setStartExpression(String startExpression) {
        this.startExpression = startExpression;
    }
    
    public String getStartExpression() {
        return startExpression;
    }
    
    public void setStartType(String startType) {
        this.startType = startType;
    }
    
    public String getStartType() {
        return startType;
    }
    
}
