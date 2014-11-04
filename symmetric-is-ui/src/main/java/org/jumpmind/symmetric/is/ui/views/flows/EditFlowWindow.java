package org.jumpmind.symmetric.is.ui.views.flows;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.config.data.ComponentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentCategory;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.ui.diagram.ConnectionEvent;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.diagram.NodeMovedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeSelectedEvent;
import org.jumpmind.symmetric.is.ui.support.ImmediateUpdateTextField;
import org.jumpmind.symmetric.is.ui.support.ResizableWindow;
import org.jumpmind.symmetric.is.ui.support.SqlEntryWindow;
import org.jumpmind.symmetric.is.ui.support.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
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

    VerticalLayout flowLayout;

    Accordion componentAccordian;

    TabSheet tabs;

    Tab palleteTab;

    Tab propertiesTab;

    VerticalLayout propertiesLayout;

    Diagram diagram;

    public EditFlowWindow() {

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setSplitPosition(350, Unit.PIXELS);
        splitPanel.setSizeFull();

        flowLayout = new VerticalLayout();
        flowLayout.setSizeFull();

        HorizontalLayout actionLayout = new HorizontalLayout();
        actionLayout.setWidth(100, Unit.PERCENTAGE);
        flowLayout.addComponent(actionLayout);

        MenuBar actionBar = new MenuBar();
        actionBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        actionBar.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        MenuItem actions = actionBar.addItem("", FontAwesome.COG, null);
        actions.addItem("Deploy", FontAwesome.COGS, null);
        actions.addItem("Execute", FontAwesome.PLAY, null);

        actionLayout.addComponent(actionBar);
        actionLayout.setComponentAlignment(actionBar, Alignment.MIDDLE_RIGHT);

        propertiesLayout = new VerticalLayout();

        tabs = new TabSheet();
        tabs.addStyleName(ValoTheme.TABSHEET_COMPACT_TABBAR);
        tabs.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);
        tabs.setSizeFull();
        palleteTab = tabs.addTab(buildPalette(), "Design Palette");
        propertiesTab = tabs.addTab(propertiesLayout, "Property Sheet");

        splitPanel.addComponents(tabs, flowLayout);

        content.addComponent(splitPanel);
        content.setExpandRatio(splitPanel, 1);

        content.addComponent(buildButtonFooter(null, new Button[] { buildCloseButton() }));

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

    public ComponentFlowVersion getComponentFlowVersion() {
        return componentFlowVersion;
    }

    public void show(ComponentFlowVersion componentFlowVersion) {
        this.componentFlowVersion = componentFlowVersion;

        setCaption("Edit Flow - Name: " + componentFlowVersion.getComponentFlow().getData().getName() + ", Version: " + componentFlowVersion.getVersion());

        populateComponentPalette();

        redrawFlow();
        
        tabs.setSelectedTab(palleteTab);

        showAtSize(.8);

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

    protected void refreshPropertiesForm(final ComponentFlowNode flowNode) {
        propertiesLayout.removeAllComponents();

        if (flowNode != null) {

            HorizontalLayout actionLayout = new HorizontalLayout();
            actionLayout.setWidth(100, Unit.PERCENTAGE);
            propertiesLayout.addComponent(actionLayout);

            MenuBar actionBar = new MenuBar();
            actionBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
            actionBar.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
            MenuItem actions = actionBar.addItem("", FontAwesome.COG, null);
            actions.addItem("Delete", new Command() {
                private static final long serialVersionUID = 1L;

                @Override
                public void menuSelected(MenuItem selectedItem) {
                    // TODO all of this should go in the service and be
                    // transactional
                    List<ComponentFlowNodeLink> links = componentFlowVersion
                            .removeComponentFlowNodeLinks(flowNode.getData().getId());
                    for (ComponentFlowNodeLink link : links) {
                        configurationService.delete(link);
                    }

                    ComponentFlowNode node = componentFlowVersion.removeComponentFlowNode(flowNode);
                    configurationService.delete(node);
                    redrawFlow();
                    refreshPropertiesForm(null);
                    tabs.setSelectedTab(palleteTab);
                }
            });

            actionLayout.addComponent(actionBar);
            actionLayout.setComponentAlignment(actionBar, Alignment.MIDDLE_RIGHT);

            FormLayout formLayout = new FormLayout();
            formLayout.setWidth(100, Unit.PERCENTAGE);
            formLayout.setMargin(false);
            formLayout.addStyleName("light");

            Label typeLabel = new Label();
            typeLabel.setCaption("Type");
            typeLabel.setValue(flowNode.getComponentVersion().getComponent().getData().getType());
            formLayout.addComponent(typeLabel);

            final TextField nameField = new TextField("Name");
            nameField.setImmediate(true);
            nameField.setNullRepresentation("");
            nameField.addBlurListener(new BlurListener() {                
                private static final long serialVersionUID = 1L;                
                @Override
                public void blur(BlurEvent event) {
                    saveName(nameField, flowNode);
                }
            });
            nameField.addShortcutListener(new ShortcutListener("nameField", KeyCode.ENTER, null) {
                private static final long serialVersionUID = 1L;
                @Override
                public void handleAction(Object sender, Object target) {
                    saveName(nameField, flowNode);
                }
            });
            nameField.setValue(flowNode.getComponentVersion().getData().getName());
            formLayout.addComponent(nameField);

            final ComponentVersion version = flowNode.getComponentVersion();
            Map<String, SettingDefinition> settings = componentFactory
                    .getSettingDefinitionsForComponentType(version.getComponent().getType());
            Set<String> keys = settings.keySet();
            for (final String key : keys) {
                final SettingDefinition definition = settings.get(key);
                boolean required = definition.required();
                String description = "Represents the " + key + " setting";
                Type type = definition.type();
                switch (type) {
                    case BOOLEAN:
                        final CheckBox checkBox = new CheckBox(definition.label());
                        checkBox.setImmediate(true);
                        checkBox.setValue(version.getBoolean(key));
                        checkBox.setRequired(required);
                        checkBox.setDescription(description);
                        checkBox.addValueChangeListener(new ValueChangeListener() {
                            
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent event) {
                                saveSetting(key, checkBox, version);
                            }
                        });
                        formLayout.addComponent(checkBox);
                        break;
                    case CHOICE:
                        break;
                    case SQL:
                        HorizontalLayout layout = new HorizontalLayout();
                        layout.setCaption(definition.label());
                        final Button button = new Button(buttonValue(version.get(key)));
                        button.addStyleName(ValoTheme.BUTTON_LINK);
                        button.addClickListener(new ClickListener() {                            
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void buttonClick(ClickEvent event) {
                                SqlEntryWindow window = new SqlEntryWindow(version.get(key)) {
                                    private static final long serialVersionUID = 1L;
                                    @Override
                                    protected boolean onClose() {
                                        saveSetting(key, editor, version);
                                        button.setCaption(buttonValue(version.get(key)));
                                        return super.onClose();
                                    }
                                };
                                window.showAtSize(.5);
                            }
                        });
                        layout.addComponent(button);
                        formLayout.addComponent(layout);
                        break;
                    case PASSWORD:
                        break;
                    case INTEGER:
                        ImmediateUpdateTextField integerField = new ImmediateUpdateTextField(definition.label()) {
                            private static final long serialVersionUID = 1L;
                            protected void save() {
                                saveSetting(key, this, version);
                            };
                        };
                        integerField.setConverter(Integer.class);
                        integerField.setValue(version.get(key));
                        integerField.setRequired(required);
                        integerField.setDescription(description);
                        formLayout.addComponent(integerField);
                        break;
                    case STRING:
                        ImmediateUpdateTextField textField = new ImmediateUpdateTextField(definition.label()) {
                            private static final long serialVersionUID = 1L;
                            protected void save() {
                                saveSetting(key, this, version);
                            };
                        };
                        textField.setValue(version.get(key));
                        textField.setRequired(required);
                        textField.setDescription(description);
                        formLayout.addComponent(textField);
                        break;
                    case XML:
                        break;
                    default:
                        break;

                }
            }

            propertiesLayout.addComponent(formLayout);
            propertiesLayout.setExpandRatio(formLayout, 1);
        }
    }
    
    protected String buttonValue(String value) {
        return isNotBlank(value) ? abbreviate(value, 30) : "Click to edit";
    }
    
    protected void saveSetting(String key, Field<?> field, ComponentVersion version) {
        SettingData data = version.findSetting(key);
        data.setValue(field.getValue() != null ? field.getValue().toString(): null);
        configurationService.save(data);
    }
    
    protected void saveName(TextField nameField, ComponentFlowNode flowNode) {
        ComponentVersion version = flowNode.getComponentVersion();
        version.getData().setName(nameField.getValue());
        configurationService.save(version);
        redrawFlow();
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
                refreshPropertiesForm(flowNode);
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
            componentVersion.getData().setName(type + " " + (countComponentsOfType(type) + 1));

            ComponentFlowNodeData nodeData = new ComponentFlowNodeData(componentVersion.getData()
                    .getId(), componentFlowVersion.getData().getId());
            componentFlowVersion.getComponentFlowNodes().add(
                    new ComponentFlowNode(componentVersion, nodeData));

            // TODO saving the entire flow for now, could save only the new node
            configurationService.save(componentFlowVersion);
            redrawFlow();
        }
    }

}
