package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeLinkData;

public class ComponentFlowNodeLink extends AbstractObject<ComponentFlowNodeLinkData> {

    private static final long serialVersionUID = 1L;

    public ComponentFlowNodeLink(String sourceNodeId, String targetNodeId) {
        this(new ComponentFlowNodeLinkData(sourceNodeId, targetNodeId));
    }

    public ComponentFlowNodeLink(ComponentFlowNodeLinkData data) {
        super(data);
    }

}
