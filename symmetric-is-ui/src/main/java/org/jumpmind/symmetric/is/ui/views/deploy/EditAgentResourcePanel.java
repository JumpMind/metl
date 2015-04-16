package org.jumpmind.symmetric.is.ui.views.deploy;

import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.VerticalSplitPanel;

@SuppressWarnings("serial")
public class EditAgentResourcePanel extends VerticalSplitPanel implements IUiPanel {

    ApplicationContext context;
    
    Resource resource;
    
    public EditAgentResourcePanel(ApplicationContext context, Resource resource) {
        this.context = context;
        this.resource = resource;
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
