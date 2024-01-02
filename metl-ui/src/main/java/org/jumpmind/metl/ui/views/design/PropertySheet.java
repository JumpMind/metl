/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.design;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.ENABLED;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.LOG_INPUT;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.LOG_OUTPUT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.model.AbstractName;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.AbstractObjectWithSettings;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.MessageType;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;
import org.jumpmind.metl.core.plugin.XMLResourceDefinition;
import org.jumpmind.metl.core.plugin.XMLSetting;
import org.jumpmind.metl.core.plugin.XMLSetting.Type;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants;
import org.jumpmind.metl.core.runtime.flow.StepRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.ImmediateUpdateTogglePasswordField;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class PropertySheet extends AbsoluteLayout {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String DUMMY_PASSWORD = "*****";

    ApplicationContext context;

    IPropertySheetChangeListener listener;

    Object value;

    Panel panel;

    TabbedPanel tabs;

    boolean readOnly;

    public PropertySheet(ApplicationContext context, TabbedPanel tabs, boolean readOnly) {
        this.tabs = tabs;
        this.context = context;
        this.readOnly = readOnly;

        setSizeFull();

        panel = new Panel();
        panel.setSizeFull();
        panel.addStyleName("noborder");
        addComponent(panel);
    }

    public void setListener(IPropertySheetChangeListener listener) {
        this.listener = listener;
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public void setSource(Object obj) {
        value = obj;
        VerticalLayout vLayout = new VerticalLayout();
        FormLayout formLayout = new FormLayout();
        formLayout.setWidth(100, Unit.PERCENTAGE);
        formLayout.setMargin(false);
        formLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);

        if (obj != null) {

            if (obj instanceof List<?>) {
                List<Object> l = (List<Object>) obj;
                if (l.size() == 1) {
                    if (l.get(0) instanceof FlowStep) {
                        obj = (FlowStep) l.get(0);
                    }
                }
            }

            if (obj instanceof FlowStep) {
                obj = ((FlowStep) obj).getComponent();
            }

            if (obj instanceof Component) {
                Component component = (Component) obj;
                context.getConfigurationService().refresh(component, true);
                addComponentProperties(formLayout, component);
            }

            if (obj instanceof Resource) {
                Resource resource = (Resource) obj;
                addButtonBar(vLayout, resource);
                addResourceProperties(formLayout, resource);
            }

            if (obj instanceof AbstractObjectWithSettings) {
                List<XMLSetting> settings = buildSettings(obj);
                if (settings != null) {
                    for (XMLSetting definition : settings) {
                        addSettingField(definition, (AbstractObjectWithSettings) obj, formLayout);
                    }
                }
            }

            if (obj instanceof Component) {
                Component component = (Component) obj;
                XMLComponentDefinition componentDefintion = context.getDefinitionFactory()
                        .getComponentDefinition(component.getProjectVersionId(), component.getType());
                addThreadCount(componentDefintion, formLayout, component);
            }

            if (obj instanceof List<?>) {
                addCommonComponentSettings(formLayout, obj);
            }

        }
        vLayout.addComponent(formLayout);
        panel.setContent(vLayout);
    }
    
    private void addButtonBar(Layout layout, Resource resource) {
        ButtonBar buttonBar = new ButtonBar();
        Button testBtn = buttonBar.addButton("Test", Icons.RUN);
        testBtn.addClickListener((event)->testResource(resource));
        testBtn.setEnabled(createResourceRuntime(resource).isTestSupported());
        layout.addComponent(buttonBar);
    }
    
    private void testResource(Resource resource) {
        try {
            createResourceRuntime(resource).test();
            CommonUiUtils.notify("Test Successful");
        } catch (Exception ex) {
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if (rootCause == null) {
                rootCause = ex;
            }
            CommonUiUtils.notify("Resource test failed. Root Cause: " + rootCause.getMessage(), com.vaadin.ui.Notification.Type.ERROR_MESSAGE);
        }
    }
    
    private IResourceRuntime createResourceRuntime(Resource resource) {
        XMLResourceDefinition definition = 
                context.getDefinitionFactory().getResourceDefintion(resource.getProjectVersionId(), resource.getType());
        TypedProperties properties = resource.toTypedProperties(definition.getSettings().getSetting());
        return AgentRuntime.create(definition, resource, properties);
    }
    
    @SuppressWarnings("unchecked")
    protected void addCommonComponentSettings(FormLayout formLayout, Object obj) {
        List<Object> list = (List<Object>) obj;
        List<Component> components = new ArrayList<Component>(list.size());
        for (Object object : list) {
            if (object instanceof FlowStep) {
                components.add(((FlowStep) object).getComponent());
            } else if (object instanceof Component) {
                components.add((Component) object);
            }
        }
        if (components.size() != 0 && !readOnly) {
            formLayout.addComponent(buildRadioButtonGroup("Enabled", ENABLED, components));
            formLayout.addComponent(buildRadioButtonGroup("Log Input", LOG_INPUT, components));
            formLayout.addComponent(buildRadioButtonGroup("Log Output", LOG_OUTPUT, components));
        }
    }

    protected RadioButtonGroup<String> buildRadioButtonGroup(String caption, String name, List<Component> components) {
        RadioButtonGroup<String> optionGroup = new RadioButtonGroup<String>(caption);
        optionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        optionGroup.setItems("ON", "OFF");
        optionGroup.addValueChangeListener((event) -> saveSetting(name, optionGroup, components));
        return optionGroup;
    }

    protected void saveSetting(String name, HasValue<String> field, List<Component> components) {
        for (final Component component : components) {
            saveSetting(name, field.getValue() != null ? Boolean.valueOf(field.getValue().equals("ON")).toString() : null, component);
        }
        if (listener != null) {
            listener.componentChanged(components);
        }
    }

    protected void addResourceProperties(FormLayout formLayout, Resource resource) {
        TextField textField = new TextField("Resource Type");
        textField.setValue(resource.getType());
        textField.setReadOnly(true);
        formLayout.addComponent(textField);
    }

    protected void addComponentProperties(FormLayout formLayout, Component component) {
        XMLComponentDefinition componentDefintion = context.getDefinitionFactory().getComponentDefinition(component.getProjectVersionId(),
                component.getType());
        addComponentName(formLayout, component);
        TextField textField = new TextField("Component Type");
        textField.setValue(componentDefintion.getName());
        textField.setReadOnly(true);
        formLayout.addComponent(textField);
        addResourceCombo(componentDefintion, formLayout, component);
        addInputModelCombo(componentDefintion, formLayout, component);
        addOutputModelCombo(componentDefintion, formLayout, component);
        addErrorHandlerCombo(componentDefintion, formLayout, component);
    }

    protected void addThreadCount(XMLComponentDefinition componentDefintion, FormLayout formLayout, final Component component) {
        if (componentDefintion.isSupportsMultipleThreads()) {
            XMLSetting setting = new XMLSetting(StepRuntime.THREAD_COUNT, "Thread Count", "1", Type.INTEGER, true);
            addSettingField(setting, component, formLayout);
        }
    }

    protected void addErrorHandlerCombo(XMLComponentDefinition componentDefinition, FormLayout formLayout, final Component component) {
        FlowStep step = getSingleFlowStep();
        if (step != null) {
            final ComboBox<FlowStep> combo = new ComboBox<FlowStep>("Error Suspense Step");
            combo.setEmptySelectionAllowed(true);
            IConfigurationService configurationService = context.getConfigurationService();
            Flow flow = configurationService.findFlow(step.getFlowId());
            String currentErrorHandlerId = component.get(ComponentSettingsConstants.ERROR_HANDLER);
            FlowStep currentValue = null;
            List<FlowStep> comboStepList = new ArrayList<FlowStep>();
            List<FlowStepLink> stepLinks = flow.findFlowStepLinksWithSource(step.getId());
            for (FlowStepLink flowStepLink : stepLinks) {
                FlowStep comboStep = flow.findFlowStepWithId(flowStepLink.getTargetStepId());
                comboStepList.add(comboStep);
                if (currentValue == null && currentErrorHandlerId != null && currentErrorHandlerId.equals(comboStep.getId())) {
                	currentValue = comboStep;
                }
            }
            combo.setItems(comboStepList);
            combo.setItemCaptionGenerator(item -> item.getName());
            if (currentValue != null) {
                combo.setValue(currentValue);
            }            
            combo.addValueChangeListener(new ValueChangeListener<FlowStep>() {
                private static final long serialVersionUID = 1L;

                @Override
                public void valueChange(ValueChangeEvent<FlowStep> event) {
                    Setting setting = step.getComponent().findSetting(ComponentSettingsConstants.ERROR_HANDLER);
                    FlowStep value = event.getValue();
                    setting.setValue(value != null ? value.getId() : null);
                    context.getConfigurationService().save(setting);
                }
            });
            combo.setReadOnly(readOnly);
            formLayout.addComponent(combo);
        }
    }

    
    protected void addOutputModelCombo(XMLComponentDefinition componentDefinition, FormLayout formLayout, final Component component) {
        FlowStep step = getSingleFlowStep();
        if (step != null) {
            IConfigurationService configurationService = context.getConfigurationService();
            String projectVersionId = step.getComponent().getProjectVersionId();
            if ((
            		(componentDefinition.getOutputMessageType() == MessageType.RELATIONAL 
	            		|| componentDefinition.getOutputMessageType() == MessageType.HIERARCHICAL 
	            		|| componentDefinition.getOutputMessageType() == MessageType.MODEL)
	                    	|| (componentDefinition.getOutputMessageType() == MessageType.ANY && componentDefinition.isShowOutputModel()))
                    && !componentDefinition.isInputOutputModelsMatch()) {
                final ComboBox<AbstractName> combo = new ComboBox<AbstractName>("Output Model");
                combo.setEmptySelectionAllowed(true);
                
                List<AbstractName> models = new ArrayList<AbstractName>();
                if (componentDefinition.getOutputMessageType() == MessageType.ANY
                		|| componentDefinition.getOutputMessageType() == MessageType.MODEL
                		||	componentDefinition.getOutputMessageType() == MessageType.RELATIONAL) {
                    models.addAll(configurationService.findRelationalModelsInProject(projectVersionId));
                }
                if (componentDefinition.getOutputMessageType() == MessageType.ANY
                		|| componentDefinition.getOutputMessageType() == MessageType.MODEL
                		|| componentDefinition.getOutputMessageType() == MessageType.HIERARCHICAL) { 
                    models.addAll(configurationService.findHierarchicalModelsInProject(projectVersionId));
                }
                
                List<ProjectVersionDepends> dependencies = configurationService.findProjectDependencies(projectVersionId);
                for (ProjectVersionDepends projectVersionDependency : dependencies) {
                    if (componentDefinition.getOutputMessageType() == MessageType.ANY
                    		|| componentDefinition.getOutputMessageType() == MessageType.MODEL
                    		|| componentDefinition.getOutputMessageType() == MessageType.RELATIONAL) {
                        models.addAll(configurationService.findRelationalModelsInProject(projectVersionDependency.getTargetProjectVersionId()));
                    }
                    if (componentDefinition.getOutputMessageType() == MessageType.ANY
                    		|| componentDefinition.getOutputMessageType() == MessageType.MODEL
                    		|| componentDefinition.getOutputMessageType() == MessageType.HIERARCHICAL) { 
                        models.addAll(configurationService.findHierarchicalModelsInProject(projectVersionDependency.getTargetProjectVersionId()));
                    }
                }

                if (models != null) {
                	combo.setItems(models);
                    for (AbstractName model : models) {
                        if (isNotBlank(component.getOutputModelId()) && component.getOutputModelId().equals(model.getId())) {
                            combo.setValue(model);
                        }
                    }
                }
                combo.addValueChangeListener(new ValueChangeListener<AbstractName>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent<AbstractName> event) {
                        AbstractName model = combo.getValue();
                        if (model != null) {
	                        component.setOutputModelId(model.getId());
	                        component.setOutputModel(configurationService.findModel(model.getId()));
                        } else {
	                        component.setOutputModelId(null);
	                        component.setOutputModel(null);
                        }
                        configurationService.save((AbstractObject) component);
                        setSource(value);
                    }
                });
                combo.setReadOnly(readOnly);
                formLayout.addComponent(combo);
            }
        }
    }

    protected void addComponentName(FormLayout formLayout, final Component component) {

        TextField textField = new TextField("Component Name");
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setValueChangeTimeout(200);
        textField.addValueChangeListener(event -> {
            component.setName(event.getValue());
            context.getConfigurationService().save(component);
            if (listener != null) {
                List<Component> components = new ArrayList<Component>(1);
                components.add(component);
                listener.componentChanged(components);
            }
        });
        textField.setValue(component.getName());
        textField.setRequiredIndicatorVisible(true);
        textField.setDescription("Name for the component on the flow");
        formLayout.addComponent(textField);
    }

    protected void addInputModelCombo(XMLComponentDefinition componentDefinition, FormLayout formLayout, final Component component) {
        FlowStep step = getSingleFlowStep();
        if (step != null) {
            IConfigurationService configurationService = context.getConfigurationService();
            String projectVersionId = step.getComponent().getProjectVersionId();
            if ((componentDefinition.getInputMessageType() == MessageType.RELATIONAL 
            		|| componentDefinition.getInputMessageType() == MessageType.HIERARCHICAL 
            		|| componentDefinition.getInputMessageType() == MessageType.MODEL)
                    || (componentDefinition.getInputMessageType() == MessageType.ANY && componentDefinition.isShowInputModel())) {
                final ComboBox<AbstractName> combo = new ComboBox<AbstractName>("Input Model");              
                combo.setEmptySelectionAllowed(true);

                List<AbstractName> models = new ArrayList<AbstractName>();
                if (componentDefinition.getInputMessageType() == MessageType.ANY
                		|| componentDefinition.getInputMessageType() == MessageType.MODEL
                		|| componentDefinition.getInputMessageType() == MessageType.RELATIONAL) {
                    models.addAll(configurationService.findRelationalModelsInProject(projectVersionId));
                } 
                if (componentDefinition.getInputMessageType() == MessageType.ANY
                		|| componentDefinition.getInputMessageType() == MessageType.MODEL
                		|| componentDefinition.getInputMessageType() == MessageType.HIERARCHICAL ) { 
                    models.addAll(configurationService.findHierarchicalModelsInProject(projectVersionId));
                }
                
                List<ProjectVersionDepends> dependencies = configurationService.findProjectDependencies(projectVersionId);
                for (ProjectVersionDepends projectVersionDependency : dependencies) {
                    if (componentDefinition.getInputMessageType() == MessageType.ANY 
                    		|| componentDefinition.getInputMessageType() == MessageType.MODEL 
                    		|| componentDefinition.getInputMessageType() == MessageType.RELATIONAL) {
                        models.addAll(configurationService.findRelationalModelsInProject(projectVersionDependency.getTargetProjectVersionId()));
                    }
                    if (componentDefinition.getInputMessageType() == MessageType.ANY 
                    		|| componentDefinition.getInputMessageType() == MessageType.MODEL 
                    		|| componentDefinition.getInputMessageType() == MessageType.HIERARCHICAL) { 
                        models.addAll(configurationService.findHierarchicalModelsInProject(projectVersionDependency.getTargetProjectVersionId()));
                    }
                }

                if (models != null) {
                	combo.setItems(models);
                    for (AbstractName model : models) {
                        if (isNotBlank(component.getInputModelId()) && component.getInputModelId().equals(model.getId())) {
                            combo.setValue(model);
                        }
                    }
                }
                combo.addValueChangeListener(new ValueChangeListener<AbstractName>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent<AbstractName> event) {
                        AbstractName model = combo.getValue();
                        if (model != null) {
                            component.setInputModelId(model.getId());
                            component.setInputModel(configurationService.findRelationalModel(model.getId()));
                        } else {
                            component.setInputModel(null);
                            component.setInputModelId(null);
                        }
                        if (componentDefinition.isInputOutputModelsMatch()) {
                            component.setOutputModel(component.getInputModel());
                            component.setOutputModelId(component.getInputModelId());
                        }
                        configurationService.save((AbstractObject) component);
                        setSource(value);
                    }
                });
                combo.setReadOnly(readOnly);
                formLayout.addComponent(combo);
            }
        }
    }

    protected void addResourceCombo(XMLComponentDefinition componentDefintion, FormLayout formLayout, final Component component) {
        if (componentDefintion == null) {
            log.error("Could not find a component defintion for: " + component.getName() + " " + component.getType());
        } else {
            IConfigurationService configurationService = context.getConfigurationService();
            FlowStep step = getSingleFlowStep();
            if (componentDefintion.getResourceCategory() != null && componentDefintion.getResourceCategory() != ResourceCategory.NONE
                    && step != null) {
                final ComboBox<Resource> resourcesCombo = new ComboBox<Resource>("Resource");
                String projectVersionId = step.getComponent().getProjectVersionId();
                Set<XMLResourceDefinition> types = context.getDefinitionFactory().getResourceDefinitions(projectVersionId,
                        componentDefintion.getResourceCategory());
                if (types != null) {
                    String[] typeStrings = new String[types.size()];
                    int i = 0;
                    for (XMLResourceDefinition type : types) {
                        typeStrings[i++] = type.getId();
                    }
                    List<Resource> resources = new ArrayList<>(configurationService.findResourcesByTypes(projectVersionId, true, typeStrings));
                    if (resources != null) {
                    	resourcesCombo.setItems(resources);

                        resourcesCombo.setValue(component.getResource());
                    }
                }
                resourcesCombo.addValueChangeListener(new ValueChangeListener<Resource>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent<Resource> event) {
                        component.setResource(resourcesCombo.getValue());
                        context.getConfigurationService().save(component);
                    }
                });

                formLayout.addComponent(resourcesCombo);
            }
        }
    }

    protected List<XMLSetting> buildSettings(Object obj) {
        if (obj instanceof Component) {
            Component component = (Component) obj;
            XMLComponentDefinition definition = context.getDefinitionFactory().getComponentDefinition(component.getProjectVersionId(),
                    component.getType());
            return definition.getSettings().getSetting();
        } else if (obj instanceof Resource) {
            Resource resource = (Resource) obj;
            XMLResourceDefinition definition = context.getDefinitionFactory().getResourceDefintion(resource.getProjectVersionId(),
                    resource.getType());         
            if (definition != null) {
                return definition.getSettings() != null ? definition.getSettings().getSetting() : Collections.emptyList();
            } else {
                throw new IllegalStateException(String.format("Could not find a resource of type: %s", resource.getType()));
            }
        } else {
            return Collections.emptyList();
        }
    }

    protected void addSettingField(final XMLSetting definition, final AbstractObjectWithSettings obj, FormLayout formLayout) {
        boolean required = definition.isRequired();
        if (definition.isVisible()) {
            Component component = null;
            if (obj instanceof Component) {
                component = (Component) obj;
            }
            String description = definition.getDescription();
            Type type = definition.getType();
            FlowStep step = null;
            switch (type) {
                case BOOLEAN:
                    final CheckBox checkBox = new CheckBox(definition.getName());
                    boolean defaultValue = false;
                    if (isNotBlank(definition.getDefaultValue())) {
                        defaultValue = Boolean.parseBoolean(definition.getDefaultValue());
                    }
                    checkBox.setValue(obj.getBoolean(definition.getId(), defaultValue));
                    checkBox.setRequiredIndicatorVisible(required);
                    checkBox.setDescription(description);

                    checkBox.addValueChangeListener(new ValueChangeListener<Boolean>() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void valueChange(ValueChangeEvent<Boolean> event) {
                            saveSetting(definition.getId(), checkBox.getValue().toString(), obj);
                            if (listener != null) {
                                List<Component> components = new ArrayList<Component>(1);
                                components.add((Component) obj);
                                listener.componentChanged(components);
                            }
                        }
                    });
                    checkBox.setReadOnly(readOnly);
                    formLayout.addComponent(checkBox);
                    break;
                case CHOICE:
                    final ComboBox<String> choice = new ComboBox<String>(definition.getName());
                    List<String> choices = definition.getChoices() != null ? definition.getChoices().getChoice() : new ArrayList<String>(0);
                    choice.setItems(choices);
                    choice.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
                    choice.setDescription(description);
                    choice.setEmptySelectionAllowed(false);
                    choice.addValueChangeListener(new ValueChangeListener<String>() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void valueChange(ValueChangeEvent<String> event) {
                            saveSetting(definition.getId(), choice.getValue(), obj);
                        }
                    });
                    choice.setReadOnly(readOnly);
                    formLayout.addComponent(choice);
                    break;
                case PASSWORD:
                    
                    ImmediateUpdateTogglePasswordField passwordField = new ImmediateUpdateTogglePasswordField(definition.getName()) {
                        protected void save(String text) {
                            if (!DUMMY_PASSWORD.equals(text)) {
                                saveSetting(definition.getId(), text, obj);
                            }
                        }
                    };
                    
                    boolean allowToggle = context.userHasPrivilege(Privilege.PASSWORD);
                    passwordField.setToggleAllowed(allowToggle);
                    
                    boolean isPasswordSet = isNotBlank(obj.get(definition.getId()));
                    if (isPasswordSet) {
                        if (allowToggle) {
                            passwordField.setValue(obj.get(definition.getId()));
                        } else {
                            passwordField.setValue(DUMMY_PASSWORD);
                        }
                    }
                    
                    passwordField.setRequiredIndicatorVisible(required);
                    passwordField.setDescription(description);
                    passwordField.setReadOnly(readOnly);
                    formLayout.addComponent(passwordField);
                    break;
                case INTEGER:
                    TextField integerField = new TextField(definition.getName());
                    integerField.setValueChangeMode(ValueChangeMode.LAZY);
                    integerField.setValueChangeTimeout(200);
                    integerField.addValueChangeListener(event -> saveSetting(definition.getId(), event.getValue(), obj));
                    new Binder<String>().forField(integerField).withConverter(new StringToIntegerConverter("Value must be an integer"))
                            .bind(value -> Integer.parseInt(value), (value, newValue) -> value = String.valueOf(newValue));
                    String integerFieldValue = obj.get(definition.getId(), definition.getDefaultValue());
                    integerField.setValue(integerFieldValue != null ? integerFieldValue : "");
                    integerField.setRequiredIndicatorVisible(required);
                    integerField.setDescription(description);
                    integerField.setReadOnly(readOnly);
                    formLayout.addComponent(integerField);
                    break;
                case TEXT:
                    TextField textField = new TextField(definition.getName());
                    textField.setValueChangeMode(ValueChangeMode.LAZY);
                    textField.setValueChangeTimeout(200);
                    textField.addValueChangeListener(event -> saveSetting(definition.getId(), event.getValue(), obj));
                    String fieldValue = obj.get(definition.getId(), definition.getDefaultValue());
                    textField.setValue(fieldValue != null ? fieldValue : "");
                    textField.setRequiredIndicatorVisible(required);
                    textField.setDescription(description);
                    textField.setReadOnly(readOnly);
                    formLayout.addComponent(textField);
                    break;
                case SOURCE_STEP:
                    step = getSingleFlowStep();
                    if (step != null) {
                        Flow flow = context.getConfigurationService().findFlow(step.getFlowId());
                        final ComboBox<FlowStep> sourceStepsCombo = new ComboBox<FlowStep>(definition.getName());

                        FlowStep currentValue = null;
                        List<FlowStep> sourceStepList = new ArrayList<FlowStep>();
                        List<FlowStepLink> sourceSteps = flow.findFlowStepLinksWithTarget(step.getId());
                        for (FlowStepLink flowStepLink : sourceSteps) {
                            FlowStep sourceStep = flow.findFlowStepWithId(flowStepLink.getSourceStepId());
                            sourceStepList.add(sourceStep);
                            if (currentValue == null && sourceStep.getId() != null && sourceStep.getId().equals(obj.get(definition.getId()))) {
                            	currentValue = sourceStep;
                            }
                        }
                        sourceStepsCombo.setItems(sourceStepList);
                        sourceStepsCombo.setItemCaptionGenerator(item -> item.getName());
                        if (currentValue != null) {
                        	sourceStepsCombo.setValue(currentValue);
                        }
                        sourceStepsCombo.setDescription(description);
                        sourceStepsCombo.setEmptySelectionAllowed(false);
                        sourceStepsCombo.setRequiredIndicatorVisible(definition.isRequired());
                        sourceStepsCombo.addValueChangeListener(new ValueChangeListener<FlowStep>() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent<FlowStep> event) {
                                saveSetting(definition.getId(), sourceStepsCombo.getValue().getId(), obj);
                            }
                        });
                        sourceStepsCombo.setReadOnly(readOnly);
                        formLayout.addComponent(sourceStepsCombo);
                    }
                    break;
                case FLOW:
                    step = getSingleFlowStep();
                    if (step != null) {
                        String projectVersionId = step.getComponent().getProjectVersionId();
                        FlowName currentValue = null;
                        List<FlowName> nameList = new ArrayList<FlowName>();
                        List<FlowName> flows = context.getConfigurationService().findFlowsInProject(projectVersionId, false);
                        final ComboBox<FlowName> combo = new ComboBox<FlowName>(definition.getName());
                        for (FlowName name : flows) {
                            if (!step.getFlowId().equals(name.getId())) {
                                nameList.add(name);
                                if (currentValue == null && name.getId() != null && name.getId().equals(obj.get(definition.getId()))) {
                                	currentValue = name;
                                }
                            }
                        }
                        combo.setItems(nameList);
                        combo.setItemCaptionGenerator(item -> item.getName());
                        if (currentValue != null) {
                        	combo.setValue(currentValue);
                        }
                        combo.setDescription(description);
                        combo.setEmptySelectionAllowed(false);
                        combo.setRequiredIndicatorVisible(definition.isRequired());
                        combo.addValueChangeListener(new ValueChangeListener<FlowName>() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent<FlowName> event) {
                                saveSetting(definition.getId(), combo.getValue().getId(), obj);
                            }
                        });
                        combo.setReadOnly(readOnly);
                        formLayout.addComponent(combo);
                    }
                    break;
                case STREAMABLE_RESOURCE:
                    formLayout.addComponent(createResourceCombo(definition, obj, ResourceCategory.STREAMABLE));
                    break;
                case DATASOURCE_RESOURCE:
                    formLayout.addComponent(createResourceCombo(definition, obj, ResourceCategory.DATASOURCE));
                    break;
                case MODEL_COLUMN:
                    if (component != null) {
                        final ComboBox<ModelAttrib> modelColumnCombo = new ComboBox<ModelAttrib>(definition.getName());                        
                        ModelAttrib currentValue = null;
                        if (component.getInputModel() instanceof RelationalModel) {
                            List<ModelEntity> entities = new ArrayList<ModelEntity>();
                            RelationalModel model = (RelationalModel) component.getInputModel();
                            if (model != null) {
                                model.sortAttributes();
                                entities.addAll(model.getModelEntities());
                            }
                            model = (RelationalModel) component.getOutputModel();
                            if (model != null) {
                                model.sortAttributes();
                                entities.addAll(model.getModelEntities());
                            }
                            AbstractObjectNameBasedSorter.sort(entities);
    
                            
                            List<ModelAttrib> attributeList = new ArrayList<ModelAttrib>();
                            Map<String, String> attributeToEntityMap = new HashMap<String, String>();
                            for (ModelEntity modelEntity : entities) {
                                for (ModelAttrib attribute : modelEntity.getModelAttributes()) {
                                    attributeList.add(attribute);
                                    attributeToEntityMap.put(attribute.getId(), modelEntity.getName());
									if (currentValue == null && attribute.getId() != null
											&& attribute.getId().equals(obj.get(definition.getId()))) {
                                    	currentValue = attribute;
                                    }
                                }
                            }
                            modelColumnCombo.setItems(attributeList);
                            modelColumnCombo.setItemCaptionGenerator(item -> attributeToEntityMap.get(item.getId()) + "." + item.getName());
                        } else {
                            //TODO: HIERARCHICAL MODEL
                        }
                        if (currentValue != null) {
                            modelColumnCombo.setValue(currentValue);
                        }
                        modelColumnCombo.setDescription(description);
                        modelColumnCombo.setEmptySelectionAllowed(definition.isRequired());
                        modelColumnCombo.setRequiredIndicatorVisible(definition.isRequired());
                        modelColumnCombo.addValueChangeListener(new ValueChangeListener<ModelAttrib>() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent<ModelAttrib> event) {
                            	ModelAttrib value = modelColumnCombo.getValue();
                                saveSetting(definition.getId(), value != null ? value.getId() : null, obj);
                            }
                        });
                        modelColumnCombo.setReadOnly(readOnly);
                        formLayout.addComponent(modelColumnCombo);
                    }
                    break;
                case SCRIPT:
                    final AceEditor editor = CommonUiUtils.createAceEditor();
                    editor.setMode(AceMode.java);
                    editor.setHeight(10, Unit.EM);
                    editor.setCaption(definition.getName());
                    editor.setShowGutter(false);
                    editor.setShowPrintMargin(false);
                    editor.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
                    editor.addValueChangeListener(new ValueChangeListener<String>() {
                        @Override
                        public void valueChange(ValueChangeEvent<String> event) {
                            Setting data = obj.findSetting(definition.getId());
                            data.setValue(event.getValue());
                            context.getConfigurationService().save(data);
                        }
                    });
                    editor.setReadOnly(readOnly);
                    formLayout.addComponent(editor);
                    break;
                case MULTILINE_TEXT:
                case XML:
                    TextArea area = new TextArea(definition.getName());
                    area.setValueChangeMode(ValueChangeMode.LAZY);
                    area.setValueChangeTimeout(200);
                    area.addValueChangeListener(event -> saveSetting(definition.getId(), event.getValue(), obj));
                    String areaValue = obj.get(definition.getId(), definition.getDefaultValue());
                    area.setValue(areaValue != null ? areaValue : "");
                    area.setRows(5);
                    area.setRequiredIndicatorVisible(required);
                    area.setDescription(description);
                    area.setReadOnly(readOnly);
                    formLayout.addComponent(area);
                    break;
                case TARGET_STEP:
                    step = getSingleFlowStep();
                    if (step != null) {
                        Flow flow = context.getConfigurationService().findFlow(step.getFlowId());
                        final ComboBox<FlowStep> targetStepsCombo = new ComboBox<FlowStep>(definition.getName());

                        FlowStep currentValue = null;
                        List<FlowStep> targetStepList = new ArrayList<FlowStep>();
                        List<FlowStepLink> targetSteps = flow.findFlowStepLinksWithSource(step.getId());
                        for (FlowStepLink flowStepLink : targetSteps) {
                            FlowStep targetStep = flow.findFlowStepWithId(flowStepLink.getTargetStepId());
                            targetStepList.add(targetStep);
							if (currentValue == null && targetStep.getId() != null
									&& targetStep.getId().equals(obj.get(definition.getId()))) {
                            	currentValue = targetStep;
                            }
                        }
                        targetStepsCombo.setItemCaptionGenerator(item -> item.getName());
                        targetStepsCombo.setItems(targetStepList);
                        if (currentValue != null) {
                        	targetStepsCombo.setValue(currentValue);
                        }
                        targetStepsCombo.setDescription(description);
                        targetStepsCombo.setEmptySelectionAllowed(true);
                        targetStepsCombo.setRequiredIndicatorVisible(definition.isRequired());
                        targetStepsCombo.addValueChangeListener(new ValueChangeListener<FlowStep>() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent<FlowStep> event) {
                            	FlowStep value = targetStepsCombo.getValue();
                                saveSetting(definition.getId(), value != null ? value.getId() : null, obj);
                            }
                        });
                        targetStepsCombo.setReadOnly(readOnly);
                        formLayout.addComponent(targetStepsCombo);
                    }
                    break;
                case CLOUD_BUCKET:
                    formLayout.addComponent(createResourceCombo(definition, obj, ResourceCategory.CLOUD_BUCKET));
                    break;
                default:
                    break;
            }
        }
    }

    protected ComboBox<Resource> createResourceCombo(XMLSetting definition,
            AbstractObjectWithSettings obj, ResourceCategory category) {
        IConfigurationService configurationService = context.getConfigurationService();
        FlowStep step = getSingleFlowStep();
        String projectVersionId = step.getComponent().getProjectVersionId();
        final ComboBox<Resource> combo = new ComboBox<Resource>(definition.getName());
        combo.setDescription(definition.getDescription());
        combo.setEmptySelectionAllowed(false);
        combo.setRequiredIndicatorVisible(definition.isRequired());
        Set<XMLResourceDefinition> types = context.getDefinitionFactory()
                .getResourceDefinitions(projectVersionId, category);
        if (types != null) {
            String[] typeStrings = new String[types.size()];
            int i = 0;
            for (XMLResourceDefinition type : types) {
                typeStrings[i++] = type.getId();
            }
            List<Resource> resources = 
                    configurationService.findResourcesByTypes(projectVersionId, true, typeStrings);

            if (resources != null) {
            	Resource currentValue = null;
                for (Resource resource : resources) {
                    if (resource.getId() != null && resource.getId().equals(obj.get(definition.getId()))) {
                    	currentValue = resource;
                    	break;
                    }
                }

                combo.setItems(resources);
                combo.setValue(currentValue);
            }
        }
        combo.setItemCaptionGenerator(item -> item.getName());
		combo.addValueChangeListener(event -> saveSetting(definition.getId(), combo.getValue().getId(), obj));
        combo.setReadOnly(readOnly);
        return combo;
    }

    protected void saveSetting(String key, String text, AbstractObjectWithSettings obj) {
        Setting data = obj.findSetting(key);
        data.setValue(text);
        context.getConfigurationService().save(data);
    }

    @SuppressWarnings("unchecked")
    protected FlowStep getSingleFlowStep() {
        FlowStep step = null;
        if (value instanceof List<?>) {
            List<Object> l = (List<Object>) value;
            if (l.size() == 1) {
                if (l.get(0) instanceof FlowStep) {
                    step = (FlowStep) l.get(0);
                }
            }
        }
        return step;
    }

}
