package org.jumpmind.metl.ui.views.deploy;

import org.jumpmind.metl.core.model.AgentResource;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.views.design.PropertySheet;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditAgentResourcePanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;
    
    AgentResource agentResource;
    
    public EditAgentResourcePanel(ApplicationContext context, AgentResource agentResource) {
        this.context = context;
        this.agentResource = agentResource;

        PropertySheet propertySheet = new PropertySheet(context);
        addComponent(propertySheet);
        propertySheet.setSource(agentResource);
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
