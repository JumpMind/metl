package org.jumpmind.symmetric.is.ui.mapping;

import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Event;

public class ConnectionEvent extends Event {

    private static final long serialVersionUID = 1L;

    String sourceId;

    String targetId;

    boolean removed;

    public ConnectionEvent(Component source, String sourceId, String targetId, boolean removed) {
        super(source);
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.removed = removed;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public boolean isRemoved() {
        return removed;
    }

}
