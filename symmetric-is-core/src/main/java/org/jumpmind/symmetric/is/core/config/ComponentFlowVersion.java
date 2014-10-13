package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;

public class ComponentFlowVersion extends AbstractObject<ComponentFlowVersionData> {

    private static final long serialVersionUID = 1L;
    
    ComponentFlow flow;
    
    List<ComponentFlowNode> componentFlowNodes;

    public ComponentFlowVersion() {
        this(new ComponentFlow(), new ComponentFlowVersionData());
    }

    public ComponentFlowVersion(ComponentFlow flow, ComponentFlowVersionData data) {
        super(data);
        this.flow = flow;
        this.componentFlowNodes = new ArrayList<ComponentFlowNode>();
    }
    
    public ComponentFlowNode findComponentFlowNodeWithId(String id) {
        for (ComponentFlowNode componentFlowNode : componentFlowNodes) {
            if (componentFlowNode.getData().getId().equals(id)) {
                return componentFlowNode;
            }
        }
        return null;
    }

    public List<ComponentFlowNode> getComponentFlowNodes() {
        return componentFlowNodes;
    }

    public void setComponentFlowNodes(List<ComponentFlowNode> componentGraphNodes) {
        this.componentFlowNodes = componentGraphNodes;
    }
    
    public ComponentFlow getFlow() {
        return flow;
    }

}
