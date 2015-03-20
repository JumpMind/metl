package org.jumpmind.symmetric.is.ui.views.design;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObjectWithSettings;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentDefinition;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.component.MessageType;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.ImmediateUpdatePasswordField;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;
import org.jumpmind.symmetric.ui.common.SqlField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class PropertySheet extends Panel implements ValueChangeListener {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    IComponentFactory componentFactory;

    IConfigurationService configurationService;

    IResourceFactory resourceFactory;

    Object value;

    public PropertySheet(ApplicationContext context) {
        this.componentFactory = context.getComponentFactory();
        this.configurationService = context.getConfigurationService();
        this.resourceFactory = context.getResourceFactory();
        setSizeFull();
        addStyleName("noborder");
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void valueChange(ValueChangeEvent event) {
        valueChange(event.getProperty().getValue());
    }

    public void valueChange(Object obj) {
        value = obj;
        FormLayout formLayout = new FormLayout();
        formLayout.setWidth(100, Unit.PERCENTAGE);
        formLayout.setMargin(false);
        formLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);

        if (obj != null) {
            if (obj instanceof FlowStep) {
                obj = ((FlowStep) obj).getComponent();
            }

            if (obj instanceof Component) {
                Component component = (Component) obj;
                configurationService.refresh(component);
                addComponentProperties(formLayout, component);
            } else if (obj instanceof Flow) {
                addFlowSpecificProperties(formLayout, (Flow) obj);
            }

            if (obj instanceof AbstractObjectWithSettings) {
                Map<String, SettingDefinition> settings = buildSettings(obj);
                Set<String> keys = settings.keySet();
                for (String key : keys) {
                    SettingDefinition definition = settings.get(key);
                    addSettingField(key, definition, (AbstractObjectWithSettings) obj, formLayout);
                }
            }
        }
        setContent(formLayout);
    }

    protected void addComponentProperties(FormLayout formLayout, Component component) {
        ComponentDefinition componentDefintion = componentFactory
                .getComponentDefinitionForComponentType(component.getType());
        addResourceCombo(componentDefintion, formLayout, component);
        addInputModelCombo(componentDefintion, formLayout, component);
        addOutputModelCombo(componentDefintion, formLayout, component);
    }

    protected void addFlowSpecificProperties(FormLayout formLayout, Flow version) {

    }

    protected void addOutputModelCombo(ComponentDefinition componentDefintion,
            FormLayout formLayout, final Component component) {
        if (value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            Flow flow = configurationService.findFlow(step.getFlowId());
            Folder folder = flow.getFolder();
            if (componentDefintion.outgoingMessage() == MessageType.ENTITY_MESSAGE) {
                final AbstractSelect combo = new ComboBox("Output Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<Model> models = configurationService.findModelsInFolder(folder);
                if (models != null) {
                    for (Model model : models) {
                        combo.addItem(model);
                    }
                    combo.setValue(component.getOutputModel());
                }
                combo.addValueChangeListener(new ValueChangeListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        component.setOutputModel((Model) combo.getValue());
                        configurationService.save(component);
                    }
                });

                formLayout.addComponent(combo);
            }
        }
    }

    protected void addInputModelCombo(ComponentDefinition componentDefintion,
            FormLayout formLayout, final Component component) {
        if (value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            Flow flow = configurationService.findFlow(step.getFlowId());
            Folder folder = flow.getFolder();
            if (componentDefintion.inputMessage() == MessageType.ENTITY_MESSAGE) {
                final AbstractSelect combo = new ComboBox("Input Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<Model> models = configurationService.findModelsInFolder(folder);
                if (models != null) {
                    for (Model model : models) {
                        combo.addItem(model);
                    }
                    combo.setValue(component.getInputModel());
                }
                combo.addValueChangeListener(new ValueChangeListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        component.setInputModel((Model) combo.getValue());
                        configurationService.save(component);
                    }
                });

                formLayout.addComponent(combo);
            }
        }
    }

    protected void addResourceCombo(ComponentDefinition componentDefintion, FormLayout formLayout,
            final Component component) {
        if (componentDefintion.resourceCategory() != null
                && componentDefintion.resourceCategory() != ResourceCategory.NONE) {
            final AbstractSelect resourcesCombo = new ComboBox("Resource");
            resourcesCombo.setImmediate(true);
            resourcesCombo.setRequired(true);
            List<String> types = resourceFactory.getResourceTypes(componentDefintion
                    .resourceCategory());
            if (types != null) {
                List<Resource> resources = configurationService.findResourcesByTypes(types
                        .toArray(new String[types.size()]));
                if (resources != null) {
                    for (Resource resource : resources) {
                        resourcesCombo.addItem(resource);
                    }

                    resourcesCombo.setValue(component.getResource());
                }
            }
            resourcesCombo.addValueChangeListener(new ValueChangeListener() {
                private static final long serialVersionUID = 1L;

                @Override
                public void valueChange(ValueChangeEvent event) {
                    component.setResource((Resource) resourcesCombo.getValue());
                    configurationService.save(component);
                }
            });

            formLayout.addComponent(resourcesCombo);
        }
    }

    protected Map<String, SettingDefinition> buildSettings(Object obj) {
        if (obj instanceof Component) {
            Component component = (Component) obj;
            return componentFactory.getSettingDefinitionsForComponentType(component.getType());
        } else if (obj instanceof Resource) {
            Resource resource = (Resource) obj;
            return resourceFactory.getSettingDefinitionsForResourceType(resource.getType());
        } else {
            return new HashMap<String, SettingDefinition>();
        }
    }

    protected void addSettingField(final String key, final SettingDefinition definition,
            final AbstractObjectWithSettings obj, FormLayout formLayout) {
        boolean required = definition.required();
        String description = "Represents the " + key + " setting";
        Type type = definition.type();
        switch (type) {
            case BOOLEAN:
                final CheckBox checkBox = new CheckBox(definition.label());
                checkBox.setImmediate(true);
                checkBox.setValue(obj.getBoolean(key));
                checkBox.setRequired(required);
                checkBox.setDescription(description);
                checkBox.addValueChangeListener(new ValueChangeListener() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        saveSetting(key, checkBox, obj);
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
                choice.setValue(obj.get(key, definition.defaultValue()));
                choice.setDescription(description);
                choice.setNullSelectionAllowed(false);
                choice.addValueChangeListener(new ValueChangeListener() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        saveSetting(key, choice, obj);
                    }
                });
                formLayout.addComponent(choice);
                break;
            case SQL:
                final SqlField sqlField = new SqlField();
                sqlField.setRequired(required);
                sqlField.setDescription(description);
                sqlField.setValue(obj.get(key));
                sqlField.setCaption(definition.label());
                sqlField.addValueChangeListener(new ValueChangeListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        saveSetting(key, sqlField, obj);
                    }
                });
                formLayout.addComponent(sqlField);
                break;
            case PASSWORD:
                ImmediateUpdatePasswordField passwordField = new ImmediateUpdatePasswordField(
                        definition.label()) {
                    private static final long serialVersionUID = 1L;

                    protected void save() {
                        saveSetting(key, this, obj);
                    };
                };
                passwordField.setValue(obj.get(key));
                passwordField.setRequired(required);
                passwordField.setDescription(description);
                formLayout.addComponent(passwordField);
                break;
            case INTEGER:
                ImmediateUpdateTextField integerField = new ImmediateUpdateTextField(
                        definition.label()) {
                    private static final long serialVersionUID = 1L;

                    protected void save() {
                        saveSetting(key, this, obj);
                    };
                };
                integerField.setConverter(Integer.class);
                integerField.setValue(obj.get(key));
                integerField.setRequired(required);
                integerField.setDescription(description);
                formLayout.addComponent(integerField);
                break;
            case STRING:
                ImmediateUpdateTextField textField = new ImmediateUpdateTextField(
                        definition.label()) {
                    private static final long serialVersionUID = 1L;

                    protected void save() {
                        saveSetting(key, this, obj);
                    };
                };
                textField.setValue(obj.get(key));
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

    protected void saveSetting(String key, Field<?> field, AbstractObjectWithSettings obj) {
        Setting data = obj.findSetting(key);
        data.setValue(field.getValue() != null ? field.getValue().toString() : null);
        configurationService.save(data);
        // componentSettingsChangedListener.componentSettingsChanges(flowNode,
        // false);
    }

}
