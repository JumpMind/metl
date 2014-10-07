package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ComponentGraphNodeData;

public class ComponentGraphNode extends AbstractObject<ComponentGraphNodeData> {

    private static final long serialVersionUID = 1L;

    public ComponentGraphNode() {
        this(new ComponentGraphNodeData());
    }

    public ComponentGraphNode(ComponentGraphNodeData data) {
        super(data);
    }

    
}
