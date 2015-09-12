package org.jumpmind.metl.ui.diagram;

import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Event;

public class NodeDoubleClickedEvent extends Event {

    private static final long serialVersionUID = 1L;

    Node node;
    
    public NodeDoubleClickedEvent(Component source, Node node) {
        super(source);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
}
