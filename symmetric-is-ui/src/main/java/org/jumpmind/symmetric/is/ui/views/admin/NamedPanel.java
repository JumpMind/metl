package org.jumpmind.symmetric.is.ui.views.admin;

import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.server.Resource;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class NamedPanel extends VerticalLayout implements IUiPanel {

    String name;
    
    Resource icon;

    public NamedPanel(String name, Resource icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Resource getIcon() {
        return icon;
    }

    public void setIcon(Resource icon) {
        this.icon = icon;
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
