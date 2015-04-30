package org.jumpmind.symmetric.is.ui.views.admin;

import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;

import com.vaadin.ui.Label;

@SuppressWarnings("serial")
public class GroupPanel extends NamedPanel {

    public GroupPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
        super("Groups");
        addComponent(new Label("Not ready yet."));
    }
    
}
