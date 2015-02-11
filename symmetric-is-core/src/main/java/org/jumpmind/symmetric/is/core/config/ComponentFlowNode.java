package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;

public class ComponentFlowNode extends AbstractObject<ComponentFlowNodeData> {

    private static final long serialVersionUID = 1L;    
    
    protected ComponentVersion componentVersion;

    public ComponentFlowNode(ComponentVersion componentVersion, ComponentFlowNodeData data) {
        super(data);
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

}
