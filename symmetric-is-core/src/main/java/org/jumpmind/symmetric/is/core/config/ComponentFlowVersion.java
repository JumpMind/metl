package org.jumpmind.symmetric.is.core.config;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ComponentFlowVersion extends AbstractObject {

    private static final long serialVersionUID = 1L;

    ComponentFlow componentFlow;

    List<ComponentFlowNode> componentFlowNodes;

    List<ComponentFlowNodeLink> componentFlowNodeLinks;
    
    String componentFlowId;
    
    String startType = StartType.MANUAL.name();
    
    String startExpression;
    
    String versionName = UUID.randomUUID().toString();

    public ComponentFlowVersion() {
        this.componentFlowNodes = new ArrayList<ComponentFlowNode>();
        this.componentFlowNodeLinks = new ArrayList<ComponentFlowNodeLink>();
    }
    
    public ComponentFlowVersion(ComponentFlow flow) {
        this();
        setComponentFlow(flow);
    }

    public ComponentFlowNode findComponentFlowNodeWithId(String id) {
        for (ComponentFlowNode componentFlowNode : componentFlowNodes) {
            if (componentFlowNode.getId().equals(id)) {
                return componentFlowNode;
            }
        }
        return null;
    }
    
    public void setComponentFlowId(String componentFlowId) {
        this.componentFlowId = componentFlowId;
    }

    public String getComponentFlowId() {
        return componentFlowId;
    }

    public void setStartExpression(String startExpression) {
        this.startExpression = startExpression;
    }
    
    public String getStartExpression() {
        return startExpression;
    }
    
    public void setStartType(String startType) {
        this.startType = startType;
    }
    
    public String getStartType() {
        return startType;
    }

    public String getFolderName() {
        return componentFlow.getFolder().getName();
    }

    public void setVersionName(String versionName) {        
        this.versionName = versionName;
    }

    public String getVersionName() {        
        return versionName;
    }

    public List<ComponentFlowNode> getComponentFlowNodes() {
        return componentFlowNodes;
    }

    public ComponentFlow getComponentFlow() {
        return componentFlow;
    }
    
    public void setComponentFlow(ComponentFlow componentFlow) {
        this.componentFlow = componentFlow;
        if (componentFlow != null) {
            componentFlowId = componentFlow.getId();
        } else {
            componentFlowId = null;
        }
    }

    public List<ComponentFlowNodeLink> getComponentFlowNodeLinks() {
        return componentFlowNodeLinks;
    }
    
    public void setName(String name) {
        setVersionName(name);
    }
    
    public String getName() {
        return versionName;
    }
    
    public ComponentFlowNode removeComponentFlowNode(ComponentFlowNode flowNode) {
        Iterator<ComponentFlowNode> i = componentFlowNodes.iterator();
        while (i.hasNext()) {
            ComponentFlowNode node = i.next();
            if (node.getId().equals(flowNode.getId())) {
                i.remove();
                return node;
            }
        }
        return null;
    }

    public List<ComponentFlowNodeLink> removeComponentFlowNodeLinks(String flowNodeId) {
        List<ComponentFlowNodeLink> links = new ArrayList<ComponentFlowNodeLink>();
        Iterator<ComponentFlowNodeLink> i = componentFlowNodeLinks.iterator();
        while (i.hasNext()) {
            ComponentFlowNodeLink link = i.next();
            if (link.getSourceNodeId().equals(flowNodeId)
                    || link.getTargetNodeId().equals(flowNodeId)) {
                i.remove();
                links.add(link);
            }
        }
        return links;
    }

    public ComponentFlowNodeLink removeComponentFlowNodeLink(String sourceNodeId, String targetNodeId) {
        Iterator<ComponentFlowNodeLink> i = componentFlowNodeLinks.iterator();
        while (i.hasNext()) {
            ComponentFlowNodeLink link = i.next();
            if (link.getSourceNodeId().equals(sourceNodeId)
                    && link.getTargetNodeId().equals(targetNodeId)) {
                i.remove();
                return link;
            }
        }
        return null;
    }
    
    public StartType asStartType() {
        if (isBlank(startType)) {
            return StartType.MANUAL;
        } else {
            return StartType.valueOf(startType);
        }
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
}
