package org.jumpmind.metl.ui.views.design;

import com.vaadin.ui.Button;

public class FlowPaletteItem extends Button {
    
    private static final long serialVersionUID = 1L;
    
    String componentId;
    boolean isShared;

    public FlowPaletteItem(String label) {
        super(label);
    }
    
    public String getComponentId() {
        return componentId;
    }
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }
    public boolean isShared() {
        return isShared;
    }
    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }
}
