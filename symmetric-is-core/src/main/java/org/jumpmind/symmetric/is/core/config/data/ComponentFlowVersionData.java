package org.jumpmind.symmetric.is.core.config.data;

public class ComponentFlowVersionData extends AbstractVersionData {

    private static final long serialVersionUID = 1L;

    String componentFlowId;
    
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

}
