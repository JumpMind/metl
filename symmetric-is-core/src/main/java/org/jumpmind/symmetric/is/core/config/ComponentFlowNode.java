package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;

public class ComponentFlowNode extends AbstractObject<ComponentFlowNodeData> {

    private static final long serialVersionUID = 1L;    
    
    protected ComponentVersion componentVersion;
    
    protected List<ComponentFlowNode> outputLinks;

    public ComponentFlowNode(ComponentVersion componentVersion, ComponentFlowNodeData data) {
        super(data);
        this.componentVersion = componentVersion;
        this.outputLinks = new ArrayList<ComponentFlowNode>();
    }

    public ComponentVersion getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(ComponentVersion componentVersion) {
        this.componentVersion = componentVersion;
    }

    public List<ComponentFlowNode> getOutputLinks() {
        return outputLinks;
    }
    
}
