package org.jumpmind.symmetric.is.ui.views.design;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.model.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.model.Connection;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentDefinition;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionCategory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;
import org.jumpmind.symmetric.ui.common.SqlField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractSelect.NewItemHandler;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ComponentSettingsSheet extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    IConfigurationService configurationService;

    ComponentFlowVersion componentFlowVersion;

    IComponentSettingsChangedListener componentSettingsChangedListener;

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    public ComponentSettingsSheet() {
    }

    protected void show(IComponentFactory componentFactory, IConnectionFactory connectionFactory,
            IConfigurationService configurationService, ComponentFlowVersion componentFlowVersion,
            IComponentSettingsChangedListener componentSettingsChangedListener) {
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
        this.componentSettingsChangedListener = componentSettingsChangedListener;
        this.configurationService = configurationService;
        this.componentFlowVersion = componentFlowVersion;
        refresh(null);
    }

    protected void refresh(ComponentFlowNode selected) {
        removeAllComponents();

        List<ComponentFlowNode> allNodes = componentFlowVersion.getComponentFlowNodes();
        final ComponentFlowNode flowNode;
        if (selected == null && allNodes.size() > 0) {
            flowNode = allNodes.get(0);
        } else {
            flowNode = selected;
        }

        HorizontalLayout actionLayout = new HorizontalLayout();
        actionLayout.setMargin(true);
        actionLayout.setWidth(100, Unit.PERCENTAGE);
        addComponent(actionLayout);


        FormLayout formLayout = new FormLayout();
        formLayout.setWidth(100, Unit.PERCENTAGE);
        formLayout.setMargin(false);
        formLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);

        addNodeCombo(formLayout, flowNode);

        if (flowNode != null) {
            ComponentVersion version = flowNode.getComponentVersion();

            Button deleteButton = new Button("Delete");
            deleteButton.addClickListener(new ClickListener() {
                private static final long serialVersionUID = 1L;                
                @Override
                public void buttonClick(ClickEvent event) {
                    ConfirmDialog.show("Delete?", String.format(
                            "Are you sure you want to delete the '%s' component?",
                            componentFlowVersion.getName()), new IConfirmListener() {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public boolean onOk() {
                            configurationService.delete(componentFlowVersion, flowNode);
                            refresh(null);
                            componentSettingsChangedListener.componentSettingsChanges(flowNode,
                                    true);
                            return true;
                        }
                    });
                }
            });
            actionLayout.addComponent(deleteButton);

            TextField typeLabel = new TextField();
            typeLabel.setCaption("Type");
            typeLabel.setValue(flowNode.getComponentVersion().getComponent().getType());
            typeLabel.setReadOnly(true);
            formLayout.addComponent(typeLabel);

            addConnectionCombo(formLayout, version);

            Map<String, SettingDefinition> settings = componentFactory
                    .getSettingDefinitionsForComponentType(version.getComponent().getType());
            Set<String> keys = settings.keySet();
            for (String key : keys) {
                SettingDefinition definition = settings.get(key);
                addSettingField(key, definition, flowNode, formLayout);
            }
        }
        addComponent(formLayout);
        setExpandRatio(formLayout, 1);
    }

    protected void addConnectionCombo(FormLayout formLayout, final ComponentVersion version) {
        ComponentDefinition componentDefintion = componentFactory
                .getComponentDefinitionForComponentType(version.getComponent().getType());
        if (componentDefintion.connectionCategory() != null
                && componentDefintion.connectionCategory() != ConnectionCategory.NONE) {
            final AbstractSelect connectionsCombo = new ComboBox("Connection");
            connectionsCombo.setImmediate(true);
            connectionsCombo.setRequired(true);
            List<String> types = connectionFactory.getConnectionTypes(componentDefintion
                    .connectionCategory());
            if (types != null) {
                List<Connection> connections = configurationService.findConnectionsByTypes(types
                        .toArray(new String[types.size()]));
                if (connections != null) {
                    for (Connection connection : connections) {
                        connectionsCombo.addItem(connection);
                    }

                    connectionsCombo.setValue(version.getConnection());
                }
            }
            connectionsCombo.addValueChangeListener(new ValueChangeListener() {
                private static final long serialVersionUID = 1L;

                @Override
                public void valueChange(ValueChangeEvent event) {
                    version.setConnection((Connection) connectionsCombo.getValue());
                    configurationService.save(version);
                }
            });

            formLayout.addComponent(connectionsCombo);
        }
    }

    protected void addNodeCombo(FormLayout formLayout, final ComponentFlowNode flowNode) {
        List<ComponentFlowNode> allNodes = componentFlowVersion.getComponentFlowNodes();
        final AbstractSelect nodeNameCombo = new ComboBox("Name");
        nodeNameCombo.setNewItemsAllowed(true);
        nodeNameCombo.setNullSelectionAllowed(false);
        nodeNameCombo.setImmediate(true);
        for (ComponentFlowNode node : allNodes) {
            nodeNameCombo.addItem(node.getId());
            nodeNameCombo.setItemCaption(node.getId(), node.getComponentVersion().getComponent().getName());
        }

        if (flowNode != null) {
            nodeNameCombo.setValue(flowNode.getId());

            nodeNameCombo.addValueChangeListener(new ValueChangeListener() {
                private static final long serialVersionUID = 1L;

                @Override
                public void valueChange(ValueChangeEvent event) {
                    List<ComponentFlowNode> allNodes = componentFlowVersion.getComponentFlowNodes();
                    for (ComponentFlowNode node : allNodes) {
                        if (node.getId().equals(nodeNameCombo.getValue())) {
                            refresh(node);
                        }
                    }
                }
            });
            nodeNameCombo.setNewItemHandler(new NewItemHandler() {
                private static final long serialVersionUID = 1L;

                @Override
                public void addNewItem(String newItemCaption) {
                    flowNode.getComponentVersion().getComponent().setName(newItemCaption);
                    nodeNameCombo.setItemCaption(flowNode.getId(), newItemCaption);
                    saveName(nodeNameCombo, flowNode);
                }
            });

        } else {
            nodeNameCombo.setEnabled(false);
        }
        formLayout.addComponent(nodeNameCombo);
    }

    protected void addSettingField(final String key, final SettingDefinition definition,
            final ComponentFlowNode flowNode, FormLayout formLayout) {
        final ComponentVersion version = flowNode.getComponentVersion();
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
                        saveSetting(key, checkBox, flowNode);
                    }
                });
                formLayout.addComponent(checkBox);
                break;
            case CHOICE:
                final AbstractSelect choice = new ComboBox(definition.label());
                choice.setImmediate(true);
                String[] choices = definition.choices();
                for (String c : choices) {
                    choice.addItem(c);
                }
                choice.setValue(version.get(key, definition.defaultValue()));
                choice.setDescription(description);
                choice.setNullSelectionAllowed(false);
                choice.addValueChangeListener(new ValueChangeListener() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        saveSetting(key, choice, flowNode);
                    }
                });
                formLayout.addComponent(choice);
                break;
            case SQL:
                final SqlField sqlField = new SqlField();
                sqlField.setRequired(required);
                sqlField.setDescription(description);
                sqlField.setValue(version.get(key));
                sqlField.setCaption(definition.label());
                sqlField.addValueChangeListener(new ValueChangeListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        saveSetting(key, sqlField, flowNode);
                    }
                });
                formLayout.addComponent(sqlField);
                break;
            case PASSWORD:
                // TODO
                break;
            case INTEGER:
                ImmediateUpdateTextField integerField = new ImmediateUpdateTextField(
                        definition.label()) {
                    private static final long serialVersionUID = 1L;

                    protected void save() {
                        saveSetting(key, this, flowNode);
                    };
                };
                integerField.setConverter(Integer.class);
                integerField.setValue(version.get(key));
                integerField.setRequired(required);
                integerField.setDescription(description);
                formLayout.addComponent(integerField);
                break;
            case STRING:
                ImmediateUpdateTextField textField = new ImmediateUpdateTextField(
                        definition.label()) {
                    private static final long serialVersionUID = 1L;

                    protected void save() {
                        saveSetting(key, this, flowNode);
                    };
                };
                textField.setValue(version.get(key));
                textField.setRequired(required);
                textField.setDescription(description);
                formLayout.addComponent(textField);
                break;
            case XML:
                // TODO - similar to sql
                break;
            default:
                break;

        }

    }

    protected void saveName(AbstractSelect nameField, ComponentFlowNode flowNode) {
        ComponentVersion version = flowNode.getComponentVersion();
        version.getComponent().setName((String) nameField.getItemCaption(nameField.getValue()));
        configurationService.save(version);
        componentSettingsChangedListener.componentSettingsChanges(flowNode, false);
    }

    protected void saveSetting(String key, Field<?> field, ComponentFlowNode flowNode) {
        Setting data = flowNode.getComponentVersion().findSetting(key);
        data.setValue(field.getValue() != null ? field.getValue().toString() : null);
        configurationService.save(data);
        componentSettingsChangedListener.componentSettingsChanges(flowNode, false);
    }

    public interface IComponentSettingsChangedListener {
        public void componentSettingsChanges(ComponentFlowNode node, boolean deleted);
    }

}
