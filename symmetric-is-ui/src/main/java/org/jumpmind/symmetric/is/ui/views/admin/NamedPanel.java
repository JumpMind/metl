package org.jumpmind.symmetric.is.ui.views.admin;

import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class NamedPanel extends VerticalLayout implements IUiPanel {

    String name;

    public NamedPanel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }
    
}
