package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;

public class ComponentFlowNode extends AbstractObject<ComponentFlowNodeData> {

    private static final long serialVersionUID = 1L;    
    
    protected ComponentVersion componentVersion;
    
    protected List<ComponentVersion> inputLinks;
    
    protected List<ComponentVersion> outputLinks;

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

    public List<ComponentVersion> getInputLinks() {
        return inputLinks;
    }

    public void setInputLinks(List<ComponentVersion> inputLinks) {
        this.inputLinks = inputLinks;
    }

    public List<ComponentVersion> getOutputLinks() {
        return outputLinks;
    }

    public void setOutputLinks(List<ComponentVersion> outputLinks) {
        this.outputLinks = outputLinks;
    }

    
    
}
