package org.jumpmind.symmetric.is.ui.views.flows;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.data.ComponentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.ui.diagram.ConnectionEvent;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.diagram.NodeMovedEvent;
import org.jumpmind.symmetric.is.ui.support.ResizableWindow;
import org.jumpmind.symmetric.is.ui.support.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
public class EditFlowWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IComponentFactory componentFactory;

    ComponentFlowVersion componentFlowVersion;

    VerticalLayout componentLayout;

    VerticalLayout flowLayout;

    public EditFlowWindow() {

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setSplitPosition(260, Unit.PIXELS);
        splitPanel.setSizeFull();

        flowLayout = new VerticalLayout();
        flowLayout.setSizeFull();

        splitPanel.addComponents(buildPalette(), flowLayout);

        content.addComponent(splitPanel);
        content.setExpandRatio(splitPanel, 1);
        content.addComponent(buildButtonFooter());

    }

    protected VerticalLayout buildPalette() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSizeFull();
        layout.setSpacing(true);

        TextField tf = new TextField();
        tf.setInputPrompt("Search Components");
        tf.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        tf.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        tf.setIcon(FontAwesome.SEARCH);
        layout.addComponent(tf);

        Accordion accordian = new Accordion();
        accordian.setSizeFull();
        componentLayout = new VerticalLayout();
        accordian.addTab(componentLayout, "Components", FontAwesome.SITEMAP);
        layout.addComponent(accordian);
        layout.setExpandRatio(accordian, 1);
        return layout;

    }
    
    public ComponentFlowVersion getComponentFlowVersion() {
        return componentFlowVersion;
    }

    public void show(ComponentFlowVersion componentFlowVersion) {
        this.componentFlowVersion = componentFlowVersion;

        setCaption("Edit Flow: " + componentFlowVersion.getFlow().getData().getName());

        populateComponentPalette();

        redrawFlow();

        resize(.8, true);

    }
    
    @Override
    protected void save() {
        configurationService.save(componentFlowVersion);
        UI.getCurrent().removeWindow(this);
    }
    
    protected void populateComponentPalette() {
        componentLayout.removeAllComponents();
        List<String> componentTypes = componentFactory.getComponentTypes();
        for (String componentType : componentTypes) {
            Button button = new Button(componentType);
            button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
            button.setWidth(100, Unit.PERCENTAGE);
            button.addClickListener(new AddComponentClickListener(componentType));
            componentLayout.addComponent(button);
        }
    }

    protected void redrawFlow() {
        flowLayout.removeAllComponents();

        Diagram diagram = new Diagram();
        diagram.addListener(new DiagramChangedListener());
        flowLayout.addComponent(diagram);

        List<ComponentFlowNode> flowNodes = componentFlowVersion.getComponentFlowNodes();
        for (ComponentFlowNode flowNode : flowNodes) {
            Node node = new Node();
            node.setText(flowNode.getComponentVersion().getComponent().getData().getType());
            node.setId(flowNode.getData().getId());
            node.setX(flowNode.getData().getX());
            node.setY(flowNode.getData().getY());
            diagram.addNode(node);
        }

    }

    class DiagramChangedListener implements Listener {
        private static final long serialVersionUID = 1L;

        @Override
        public void componentEvent(Event e) {
            if (e instanceof NodeMovedEvent) {
                NodeMovedEvent event = (NodeMovedEvent) e;
                Node node = event.getNode();
                ComponentFlowNode flowNode = componentFlowVersion.findComponentFlowNodeWithId(node.getId());
                if (flowNode != null) {
                    flowNode.getData().setX(node.getX());
                    flowNode.getData().setY(node.getY());
                }
            } else if (e instanceof ConnectionEvent) {
                //ConnectionEvent event = (ConnectionEvent) e;
            }
        }
    }

    class AddComponentClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        String type;

        public AddComponentClickListener(String type) {
            this.type = type;
        }

        @Override
        public void buttonClick(ClickEvent event) {
            Component component = new Component(new ComponentData(type, false));
            ComponentVersion componentVersion = new ComponentVersion(component, null,
                    new ComponentVersionData());
            ComponentFlowNodeData nodeData = new ComponentFlowNodeData(componentVersion.getData()
                    .getId(), componentFlowVersion.getData().getId());
            componentFlowVersion.getComponentFlowNodes().add(
                    new ComponentFlowNode(componentVersion, nodeData));
            redrawFlow();
        }
    }

}
