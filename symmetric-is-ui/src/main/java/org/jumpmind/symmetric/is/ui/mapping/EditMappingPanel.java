package org.jumpmind.symmetric.is.ui.mapping;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.diagram.LinkEvent;
import org.jumpmind.symmetric.is.ui.diagram.LinkSelectedEvent;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.diagram.NodeDoubleClickedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeMovedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeSelectedEvent;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Component.Event;
import com.vaadin.ui.Component.Listener;

@SuppressWarnings("serial")
public class EditMappingPanel extends VerticalLayout implements IUiPanel {

	ApplicationContext context;
	
	Component component;
	
	MappingDiagram diagram;

	public EditMappingPanel(ApplicationContext context, Component component) {
		this.context = context;
		this.component = component;
		
		ButtonBar buttonBar = new ButtonBar();
		addComponent(buttonBar);
		Button autoMapButton = buttonBar.addButton("Auto Map", FontAwesome.FLASH);			
		Button removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);

		diagram = new MappingDiagram(context, component);
		addComponent(diagram);
		setExpandRatio(diagram, 1.0f);		
	}

	@Override
	public boolean closing() {
		return true;
	}

	@Override
	public void showing() {
	}
}
