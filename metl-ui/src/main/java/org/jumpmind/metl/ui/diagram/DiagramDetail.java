package org.jumpmind.metl.ui.diagram;

import java.util.ArrayList;
import java.util.List;

public class DiagramDetail {
    public List<Node> nodes = new ArrayList<Node>();
    public List<String> selectedNodeIds = new ArrayList<String>();

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<String> getSelectedNodeIds() {
        return selectedNodeIds;
    }

    public void setSelectedNodeIds(List<String> selectedNodeIds) {
        this.selectedNodeIds = selectedNodeIds;
    }

}
