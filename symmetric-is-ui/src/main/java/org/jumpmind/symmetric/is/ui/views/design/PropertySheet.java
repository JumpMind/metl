package org.jumpmind.symmetric.is.ui.views.design;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObjectWithSettings;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
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
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ImmediateUpdatePasswordField;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextArea;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
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
    
    IPropertySheetChangeListener listener;

    Object value;

    public PropertySheet(ApplicationContext context) {
        this.componentFactory = context.getComponentFactory();
        this.configurationService = context.getConfigurationService();
        this.resourceFactory = context.getResourceFactory();
        setSizeFull();
        addStyleName("noborder");
    }
    
    public void setListener(IPropertySheetChangeListener listener) {
        this.listener = listener;
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
        addComponentName(formLayout, component);
        addResourceCombo(componentDefintion, formLayout, component);
        addInputModelCombo(componentDefintion, formLayout, component);
        addOutputModelCombo(componentDefintion, formLayout, component);
    }

    protected void addOutputModelCombo(ComponentDefinition componentDefintion,
            FormLayout formLayout, final Component component) {
        if (value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            Flow flow = configurationService.findFlow(step.getFlowId());
            String projectVersionId = flow.getProjectVersionId();
            if ((componentDefintion.outgoingMessage() == MessageType.ENTITY || componentDefintion
                    .outgoingMessage() == MessageType.ANY)
                    && !componentDefintion.inputOutputModelsMatch()) {
                final AbstractSelect combo = new ComboBox("Output Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<Model> models = configurationService.findModelsInProject(projectVersionId);
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

    protected void addComponentName(FormLayout formLayout, final Component component) {
        
        ImmediateUpdateTextField textField = new ImmediateUpdateTextField(
                "Component Name") {
            private static final long serialVersionUID = 1L;

            protected void save() {
                component.setName(this.getValue());
                configurationService.save(component);
                if (listener != null) {
                    listener.componentNameChanged(component);
                }
            };
        };
        textField.setValue(component.getName());
        textField.setRequired(true);
        textField.setDescription("Name for the component on the flow");
        formLayout.addComponent(textField);
    }
    
    protected void addInputModelCombo(ComponentDefinition componentDefintion,
            FormLayout formLayout, final Component component) {
        if (value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            Flow flow = configurationService.findFlow(step.getFlowId());
            String projectVersionId = flow.getProjectVersionId();
            if (componentDefintion.inputMessage() == MessageType.ENTITY
                    || componentDefintion.inputMessage() == MessageType.ANY) {
                final AbstractSelect combo = new ComboBox("Input Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<Model> models = configurationService.findModelsInProject(projectVersionId);
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
                && componentDefintion.resourceCategory() != ResourceCategory.NONE
                && value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            final AbstractSelect resourcesCombo = new ComboBox("Resource");
            resourcesCombo.setImmediate(true);
            resourcesCombo.setRequired(true);
            List<String> types = resourceFactory.getResourceTypes(componentDefintion
                    .resourceCategory());
            Flow flow = configurationService.findFlow(step.getFlowId());
            String projectVersionId = flow.getProjectVersionId();
            if (types != null) {
                List<Resource> resources = configurationService.findResourcesByTypes(
                        projectVersionId, types.toArray(new String[types.size()]));
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
                boolean defaultValue = false;
                if (isNotBlank(definition.defaultValue())) {
                    defaultValue = Boolean.parseBoolean(definition.defaultValue());
                }
                checkBox.setValue(obj.getBoolean(key, defaultValue));
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
            case PASSWORD:
                ImmediateUpdatePasswordField passwordField = new ImmediateUpdatePasswordField(
                        definition.label()) {
                    private static final long serialVersionUID = 1L;

                    protected void save() {
                        saveSetting(key, this, obj);
                    };
                };
                passwordField.setValue(obj.get(key, definition.defaultValue()));
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
                integerField.setValue(obj.get(key, definition.defaultValue()));
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
                textField.setValue(obj.get(key, definition.defaultValue()));
                textField.setRequired(required);
                textField.setDescription(description);
                formLayout.addComponent(textField);
                break;
            case SOURCE_STEP:
                if (value instanceof FlowStep) {
                    FlowStep step = (FlowStep) value;
                    Flow flow = configurationService.findFlow(step.getFlowId());
                    final AbstractSelect sourceStepsCombo = new ComboBox(definition.label());
                    sourceStepsCombo.setImmediate(true);

                    List<FlowStepLink> sourceSteps = flow.findFlowStepLinksWithTarget(step.getId());
                    for (FlowStepLink flowStepLink : sourceSteps) {
                        FlowStep sourceStep = flow.findFlowStepWithId(flowStepLink
                                .getSourceStepId());
                        sourceStepsCombo.addItem(sourceStep.getId());
                        sourceStepsCombo.setItemCaption(sourceStep.getId(), sourceStep.getName());
                    }
                    sourceStepsCombo.setValue(obj.get(key));
                    sourceStepsCombo.setDescription(description);
                    sourceStepsCombo.setNullSelectionAllowed(false);
                    sourceStepsCombo.addValueChangeListener(new ValueChangeListener() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void valueChange(ValueChangeEvent event) {
                            saveSetting(key, sourceStepsCombo, obj);
                        }
                    });
                    formLayout.addComponent(sourceStepsCombo);
                }
                break;
            case SCRIPT:
                final AceEditor editor = CommonUiUtils.createAceEditor();
                editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
                editor.setTextChangeTimeout(200);
                editor.setMode(AceMode.java);
                editor.setHeight(10, Unit.EM);
                editor.setCaption(definition.label());
                editor.setShowGutter(false);
                editor.setShowPrintMargin(false);
                editor.setValue(obj.get(key, definition.defaultValue()));
                editor.addTextChangeListener(new TextChangeListener() {                    
                    @Override
                    public void textChange(TextChangeEvent event) {                        
                        Setting data = obj.findSetting(key);
                        data.setValue(event.getText());
                        configurationService.save(data);
                    }
                });
                formLayout.addComponent(editor);
                break;
            case TEXT:
            case XML:
                ImmediateUpdateTextArea area = new ImmediateUpdateTextArea(definition.label()) {
                    private static final long serialVersionUID = 1L;

                    protected void save() {
                        saveSetting(key, this, obj);
                    };
                };
                area.setValue(obj.get(key, definition.defaultValue()));
                area.setRows(5);
                area.setRequired(required);
                area.setDescription(description);
                formLayout.addComponent(area);
                break;
            default:
                break;

        }

    }

    protected void saveSetting(String key, Field<?> field, AbstractObjectWithSettings obj) {
        Setting data = obj.findSetting(key);
        data.setValue(field.getValue() != null ? field.getValue().toString() : null);
        configurationService.save(data);
    }

}
