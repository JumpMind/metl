package org.jumpmind.symmetric.is.ui.diagram;

import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Event;

public class LinkSelectedEvent extends Event {

    private static final long serialVersionUID = 1L;

    String sourceNodeId;

    String targetNodeId;

    public LinkSelectedEvent(Component source, String sourceNodeId, String targetNodeId) {
        super(source);
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

}
