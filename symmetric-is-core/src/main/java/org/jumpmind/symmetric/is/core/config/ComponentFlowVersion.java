package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;

public class ComponentFlowVersion extends AbstractObject<ComponentFlowVersionData> {

    private static final long serialVersionUID = 1L;
    List<ComponentFlowNode> componentGraphNodes;

    public ComponentFlowVersion() {
        this(new ComponentFlowVersionData());
    }

    public ComponentFlowVersion(ComponentFlowVersionData data) {
        super(data);
        this.componentGraphNodes = new ArrayList<ComponentFlowNode>();
    }

    public List<ComponentFlowNode> getComponentGraphNodes() {
        return componentGraphNodes;
    }

    public void setComponentGraphNodes(List<ComponentFlowNode> componentGraphNodes) {
        this.componentGraphNodes = componentGraphNodes;
    }

}
