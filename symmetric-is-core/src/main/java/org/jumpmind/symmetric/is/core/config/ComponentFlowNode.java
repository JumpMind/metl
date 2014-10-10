package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;

public class ComponentFlowNode extends AbstractObject<ComponentFlowNodeData> {

    private static final long serialVersionUID = 1L;    
    
    protected ComponentVersion componentVersion;
    
    protected List<ComponentFlowNode> inputLinks;
    
    protected List<ComponentFlowNode> outputLinks;

    public ComponentFlowNode() {
        this(new ComponentFlowNodeData());
    }

    public ComponentFlowNode(ComponentFlowNodeData data) {
        super(data);
    }

    public ComponentVersion getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(ComponentVersion componentVersion) {
        this.componentVersion = componentVersion;
    }

    public List<ComponentFlowNode> getInputLinks() {
        return inputLinks;
    }

    public void setInputLinks(List<ComponentFlowNode> inputLinks) {
        this.inputLinks = inputLinks;
    }

    public List<ComponentFlowNode> getOutputLinks() {
        return outputLinks;
    }

    public void setOutputLinks(List<ComponentFlowNode> outputLinks) {
        this.outputLinks = outputLinks;
    }

    
    
}
