package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentGraphVersionData;

public class ComponentGraphVersion extends AbstractObject<ComponentGraphVersionData> {

    private static final long serialVersionUID = 1L;
    List<ComponentGraphNode> componentGraphNodes;

    public ComponentGraphVersion() {
        this(new ComponentGraphVersionData());
    }

    public ComponentGraphVersion(ComponentGraphVersionData data) {
        super(data);
        this.componentGraphNodes = new ArrayList<ComponentGraphNode>();
    }

    public List<ComponentGraphNode> getComponentGraphNodes() {
        return componentGraphNodes;
    }

    public void setComponentGraphNodes(List<ComponentGraphNode> componentGraphNodes) {
        this.componentGraphNodes = componentGraphNodes;
    }

}
