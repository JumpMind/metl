package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeLinkData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;

public class ComponentFlowVersion extends AbstractObject<ComponentFlowVersionData> {

    private static final long serialVersionUID = 1L;

    ComponentFlow flow;

    List<ComponentFlowNode> componentFlowNodes;

    List<ComponentFlowNodeLinkData> componentFlowNodeLinkDatas;

    public ComponentFlowVersion() {
        this(new ComponentFlow(), new ComponentFlowVersionData());
    }

    public ComponentFlowVersion(ComponentFlow flow, ComponentFlowVersionData data) {
        super(data);
        this.flow = flow;
        this.componentFlowNodes = new ArrayList<ComponentFlowNode>();
        this.componentFlowNodeLinkDatas = new ArrayList<ComponentFlowNodeLinkData>();
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

    public ComponentFlow getFlow() {
        return flow;
    }

    public List<ComponentFlowNodeLinkData> getComponentFlowNodeLinkDatas() {
        return componentFlowNodeLinkDatas;
    }

    public void removeComponentFlowNodeLinkData(String sourceNodeId, String targetNodeId) {
        Iterator<ComponentFlowNodeLinkData> i = componentFlowNodeLinkDatas.iterator();
        while (i.hasNext()) {
            ComponentFlowNodeLinkData link = i.next();
            if (link.getSourceNodeId().equals(sourceNodeId)
                    && link.getTargetNodeId().equals(targetNodeId)) {
                i.remove();
            }

        }
    }

}
