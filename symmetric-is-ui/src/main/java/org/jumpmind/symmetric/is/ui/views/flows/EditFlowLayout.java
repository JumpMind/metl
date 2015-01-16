package org.jumpmind.symmetric.is.ui.views.flows;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jumpmind.symmetric.ui.common.CommonUiUtils.createComboBox;
import static org.jumpmind.symmetric.ui.common.CommonUiUtils.createSeparator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.StartType;
import org.jumpmind.symmetric.is.core.config.data.ComponentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentCategory;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.ui.diagram.ConnectionEvent;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.diagram.NodeMovedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeSelectedEvent;
import org.jumpmind.symmetric.is.ui.support.DesignAgentSelect;
import org.jumpmind.symmetric.is.ui.views.flows.ComponentSettingsSheet.IComponentSettingsChangedListener;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.vaadin.maddon.layouts.MHorizontalLayout;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditFlowLayout extends VerticalLayout implements IComponentSettingsChangedListener,
        IUiPanel {

    private static final long serialVersionUID = 1L;

    IConfigurationService configurationService;

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    ComponentFlowVersion componentFlowVersion;

    VerticalLayout flowLayout;

    Accordion componentAccordian;

    TabSheet tabs;

    Tab palleteTab;

    Tab propertiesTab;

    ComponentSettingsSheet componentSettingsSheet;

    Diagram diagram;

    Button deployButton;

    Button executeButton;

    AbstractSelect startType;

    TextField startExpression;

    DesignAgentSelect designAgentSelect;

    public EditFlowLayout(ComponentFlowVersion componentFlowVersion,
            IConfigurationService configurationService, IComponentFactory componentFactory,
            IConnectionFactory connectionFactory, DesignAgentSelect designAgentSelect) {

        this.componentFlowVersion = componentFlowVersion;
        this.configurationService = configurationService;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
        this.designAgentSelect = designAgentSelect;

        VerticalLayout content = this;

        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setSplitPosition(350, Unit.PIXELS);
        splitPanel.setSizeFull();

        flowLayout = new VerticalLayout();
        flowLayout.setSizeFull();

        componentSettingsSheet = new ComponentSettingsSheet();

        tabs = new TabSheet();
        tabs.addStyleName(ValoTheme.TABSHEET_COMPACT_TABBAR);
        tabs.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);
        tabs.setSizeFull();
        palleteTab = tabs.addTab(buildPalette(), "Design Palette");
        propertiesTab = tabs.addTab(componentSettingsSheet, "Property Sheet");

        splitPanel.addComponents(tabs, flowLayout);

        content.addComponent(splitPanel);
        content.setExpandRatio(splitPanel, 1);

        MHorizontalLayout actionLayout = new MHorizontalLayout().withMargin(true).withSpacing(true);
        flowLayout.addComponent(actionLayout);

        deployButton = new Button("Deploy");

        executeButton = new Button("Execute");

        startExpression = new TextField("Cron Expression");
        startExpression.setVisible(componentFlowVersion.getStartType() == StartType.SCHEDULED_CRON);
        startExpression.setImmediate(true);
        startExpression.setTextChangeTimeout(500);
        startExpression.setTextChangeEventMode(TextChangeEventMode.LAZY);
        startExpression.setValue(componentFlowVersion.getStartExpression());
        startExpression.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (validateCron()) {
                    EditFlowLayout.this.componentFlowVersion.getData().setStartExpression(
                            startExpression.getValue());
                    EditFlowLayout.this.configurationService
                            .save(EditFlowLayout.this.componentFlowVersion);
                }
            }
        });

        startType = createComboBox("Start Type");
        startType.setWidth(12, Unit.EM);
        startType.addItems(Arrays.asList(StartType.values()));
        startType.setValue(componentFlowVersion.getStartType());
        startType.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (startType.getValue() == StartType.SCHEDULED_CRON) {
                    if (!validateCron()) {
                        startExpression.setValue("0 0 * * * *");
                    }
                    startExpression.setVisible(true);
                } else {
                    startExpression.setVisible(false);
                }

                EditFlowLayout.this.componentFlowVersion.getData().setStartType(
                        startType.getValue().toString());
                EditFlowLayout.this.configurationService
                        .save(EditFlowLayout.this.componentFlowVersion);
            }
        });

        actionLayout
                .add(deployButton, executeButton, createSeparator(), startType, startExpression);
        actionLayout.alignAll(Alignment.BOTTOM_LEFT);

        setCaption("Name: " + componentFlowVersion.getComponentFlow().getData().getName()
                + ", Version: " + componentFlowVersion.getVersion());

        populateComponentPalette();

        this.componentSettingsSheet.show(componentFactory, connectionFactory, configurationService,
                componentFlowVersion, this);

        redrawFlow();

        tabs.setSelectedTab(palleteTab);

    }

    protected boolean validateCron() {
        try {
            if (isNotBlank(startExpression.getValue())) {
                new CronSequenceGenerator(startExpression.getValue());
                return true;
            }
        } catch (IllegalArgumentException ex) {
        }
        return false;

    }

    @Override
    public void showing() {
    }

    @Override
    public boolean closing() {
        return true;
    }

    public ComponentFlowVersion getComponentFlowVersion() {
        return componentFlowVersion;
    }

    @Override
    public void componentSettingsChanges(ComponentFlowNode node, boolean deleted) {
        redrawFlow();
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

        componentAccordian = new Accordion();
        componentAccordian.setSizeFull();
        layout.addComponent(componentAccordian);
        layout.setExpandRatio(componentAccordian, 1);
        return layout;

    }

    protected void populateComponentPalette() {
        componentAccordian.removeAllComponents();
        Map<ComponentCategory, List<String>> componentTypesByCategory = componentFactory
                .getComponentTypes();
        for (ComponentCategory category : componentTypesByCategory.keySet()) {
            List<String> componentTypes = componentTypesByCategory.get(category);
            VerticalLayout componentLayout = new VerticalLayout();
            componentAccordian.addTab(componentLayout, category.name() + "S");
            if (componentTypes != null) {
                for (String componentType : componentTypes) {
                    Button button = new Button(componentType);
                    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
                    button.addStyleName("leftAligned");
                    button.setWidth(100, Unit.PERCENTAGE);
                    button.addClickListener(new AddComponentClickListener(componentType));
                    componentLayout.addComponent(button);
                }
            }
        }
    }

    protected void redrawFlow() {
        if (diagram != null) {
            flowLayout.removeComponent(diagram);
        }

        diagram = new Diagram();
        diagram.addListener(new DiagramChangedListener());
        flowLayout.addComponent(diagram);
        flowLayout.setExpandRatio(diagram, 1);

        List<ComponentFlowNodeLink> links = componentFlowVersion.getComponentFlowNodeLinks();

        List<ComponentFlowNode> flowNodes = componentFlowVersion.getComponentFlowNodes();
        for (ComponentFlowNode flowNode : flowNodes) {
            Node node = new Node();
            String name = flowNode.getComponentVersion().getData().getName();
            String type = flowNode.getComponentVersion().getComponent().getData().getType();
            node.setText(name + "<br><i>" + type + "</i>");
            node.setId(flowNode.getData().getId());
            node.setX(flowNode.getData().getX());
            node.setY(flowNode.getData().getY());
            diagram.addNode(node);

            for (ComponentFlowNodeLink link : links) {
                if (link.getData().getSourceNodeId().equals(node.getId())) {
                    node.getTargetNodeIds().add(link.getData().getTargetNodeId());
                }
            }

        }

    }

    private int countComponentsOfType(String type) {
        int count = 0;
        List<ComponentFlowNode> nodes = componentFlowVersion.getComponentFlowNodes();
        for (ComponentFlowNode componentFlowNode : nodes) {
            if (componentFlowNode.getComponentVersion().getComponent().getData().getType()
                    .equals(type)) {
                count++;
            }
        }
        return count;
    }

    class DiagramChangedListener implements Listener {
        private static final long serialVersionUID = 1L;

        @Override
        public void componentEvent(Event e) {
            if (e instanceof NodeSelectedEvent) {
                NodeSelectedEvent event = (NodeSelectedEvent) e;
                Node node = event.getNode();
                ComponentFlowNode flowNode = componentFlowVersion.findComponentFlowNodeWithId(node
                        .getId());
                componentSettingsSheet.refresh(flowNode);
                tabs.setSelectedTab(propertiesTab);

            } else if (e instanceof NodeMovedEvent) {
                NodeMovedEvent event = (NodeMovedEvent) e;
                Node node = event.getNode();
                ComponentFlowNode flowNode = componentFlowVersion.findComponentFlowNodeWithId(node
                        .getId());
                if (flowNode != null) {
                    flowNode.getData().setX(node.getX());
                    flowNode.getData().setY(node.getY());
                }
                configurationService.save(componentFlowVersion);

            } else if (e instanceof ConnectionEvent) {
                ConnectionEvent event = (ConnectionEvent) e;
                if (!event.isRemoved()) {
                    componentFlowVersion.getComponentFlowNodeLinks().add(
                            new ComponentFlowNodeLink(event.getSourceNodeId(), event
                                    .getTargetNodeId()));
                    configurationService.save(componentFlowVersion);
                } else {
                    ComponentFlowNodeLink link = componentFlowVersion.removeComponentFlowNodeLink(
                            event.getSourceNodeId(), event.getTargetNodeId());
                    if (link != null) {
                        configurationService.delete(link);
                    }

                }
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

            componentVersion.setName(type + " " + (countComponentsOfType(type) + 1));

            ComponentFlowNodeData nodeData = new ComponentFlowNodeData(componentVersion.getData()
                    .getId(), componentFlowVersion.getData().getId());

            ComponentFlowNode componentFlowNode = new ComponentFlowNode(componentVersion, nodeData);
            componentFlowVersion.getComponentFlowNodes().add(componentFlowNode);

            configurationService.save(componentFlowNode);

            redrawFlow();

            componentSettingsSheet.refresh(componentFlowNode);

            tabs.setSelectedTab(componentSettingsSheet);
        }
    }

}
