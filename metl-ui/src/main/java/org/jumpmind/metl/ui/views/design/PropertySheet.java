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
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.AbstractObjectWithSettings;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
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
import org.jumpmind.metl.core.runtime.flow.StepRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.ImmediateUpdateTogglePasswordField;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextArea;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
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
            formLayout.addComponent(buildOptionGroup("Enabled", ENABLED, components));
            formLayout.addComponent(buildOptionGroup("Log Input", LOG_INPUT, components));
            formLayout.addComponent(buildOptionGroup("Log Output", LOG_OUTPUT, components));
        }
    }

    protected OptionGroup buildOptionGroup(String caption, String name, List<Component> components) {
        OptionGroup optionGroup = new OptionGroup(caption);
        optionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        optionGroup.setImmediate(true);
        optionGroup.addItem("ON");
        optionGroup.addItem("OFF");
        optionGroup.addValueChangeListener((event) -> saveSetting(name, optionGroup, components));
        return optionGroup;
    }

    protected void saveSetting(String name, Field<?> field, List<Component> components) {
        for (final Component component : components) {
            saveSetting(name, field.getValue() != null ? Boolean.valueOf(field.getValue().toString().equals("ON")).toString() : null,
                    component);
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
    }

    protected void addThreadCount(XMLComponentDefinition componentDefintion, FormLayout formLayout, final Component component) {
        if (componentDefintion.isSupportsMultipleThreads()) {
            XMLSetting setting = new XMLSetting(StepRuntime.THREAD_COUNT, "Thread Count", "1", Type.INTEGER, true);
            addSettingField(setting, component, formLayout);
        }
    }

    protected void addOutputModelCombo(XMLComponentDefinition componentDefintion, FormLayout formLayout, final Component component) {
        FlowStep step = getSingleFlowStep();
        if (step != null) {
            IConfigurationService configurationService = context.getConfigurationService();
            String projectVersionId = step.getComponent().getProjectVersionId();
            if ((componentDefintion.getOutputMessageType() == MessageType.ENTITY
                    || (componentDefintion.getOutputMessageType() == MessageType.ANY && componentDefintion.isShowOutputModel()))
                    && !componentDefintion.isInputOutputModelsMatch()) {
                final AbstractSelect combo = new ComboBox("Output Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<ModelName> models = new ArrayList<>(configurationService.findModelsInProject(projectVersionId));
                List<ProjectVersionDepends> dependencies = configurationService.findProjectDependencies(projectVersionId);
                for (ProjectVersionDepends projectVersionDependency : dependencies) {
                    models.addAll(configurationService.findModelsInProject(projectVersionDependency.getTargetProjectVersionId()));
                }

                if (models != null) {
                    for (ModelName model : models) {
                        combo.addItem(model);
                        if (isNotBlank(component.getOutputModelId()) && component.getOutputModelId().equals(model.getId())) {
                            combo.setValue(model);
                        }
                    }
                }
                combo.addValueChangeListener(new ValueChangeListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        ModelName model = (ModelName) combo.getValue();
                        if (model != null) {
                            component.setOutputModel(configurationService.findModel(model.getId()));
                        } else {
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

        ImmediateUpdateTextField textField = new ImmediateUpdateTextField("Component Name") {
            private static final long serialVersionUID = 1L;

            protected void save(String text) {
                component.setName(text);
                context.getConfigurationService().save(component);
                if (listener != null) {
                    List<Component> components = new ArrayList<Component>(1);
                    components.add(component);
                    listener.componentChanged(components);
                }
            };
        };
        textField.setValue(component.getName());
        textField.setRequired(true);
        textField.setDescription("Name for the component on the flow");
        formLayout.addComponent(textField);
    }

    protected void addInputModelCombo(XMLComponentDefinition componentDefintion, FormLayout formLayout, final Component component) {
        FlowStep step = getSingleFlowStep();
        if (step != null) {
            IConfigurationService configurationService = context.getConfigurationService();
            String projectVersionId = step.getComponent().getProjectVersionId();
            if (componentDefintion.getInputMessageType() == MessageType.ENTITY
                    || (componentDefintion.getInputMessageType() == MessageType.ANY && componentDefintion.isShowInputModel())) {
                final AbstractSelect combo = new ComboBox("Input Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<ModelName> models = new ArrayList<>(configurationService.findModelsInProject(projectVersionId));
                List<ProjectVersionDepends> dependencies = configurationService.findProjectDependencies(projectVersionId);
                for (ProjectVersionDepends projectVersionDependency : dependencies) {
                    models.addAll(configurationService.findModelsInProject(projectVersionDependency.getTargetProjectVersionId()));
                }

                if (models != null) {
                    for (ModelName model : models) {
                        combo.addItem(model);
                        if (isNotBlank(component.getInputModelId()) && component.getInputModelId().equals(model.getId())) {
                            combo.setValue(model);
                        }
                    }
                }
                combo.addValueChangeListener(new ValueChangeListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        ModelName model = (ModelName) combo.getValue();
                        if (model != null) {
                            component.setInputModel(configurationService.findModel(model.getId()));
                        } else {
                            component.setInputModel(null);
                        }
                        if (componentDefintion.isInputOutputModelsMatch()) {
                            component.setOutputModel(component.getInputModel());
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
                final AbstractSelect resourcesCombo = new ComboBox("Resource");
                resourcesCombo.setImmediate(true);
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
                    checkBox.setImmediate(true);
                    boolean defaultValue = false;
                    if (isNotBlank(definition.getDefaultValue())) {
                        defaultValue = Boolean.parseBoolean(definition.getDefaultValue());
                    }
                    checkBox.setValue(obj.getBoolean(definition.getId(), defaultValue));
                    checkBox.setRequired(required);
                    checkBox.setDescription(description);

                    checkBox.addValueChangeListener(new ValueChangeListener() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void valueChange(ValueChangeEvent event) {
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
                    final AbstractSelect choice = new ComboBox(definition.getName());
                    choice.setImmediate(true);
                    List<String> choices = definition.getChoices() != null ? definition.getChoices().getChoice() : new ArrayList<String>(0);
                    for (String c : choices) {
                        choice.addItem(c);
                    }
                    choice.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
                    choice.setDescription(description);
                    choice.setNullSelectionAllowed(false);
                    choice.addValueChangeListener(new ValueChangeListener() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void valueChange(ValueChangeEvent event) {
                            saveSetting(definition.getId(), (String) choice.getValue(), obj);
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
                    
                    passwordField.setRequired(required);
                    passwordField.setDescription(description);
                    passwordField.setReadOnly(readOnly);
                    formLayout.addComponent(passwordField);
                    break;
                case INTEGER:
                    ImmediateUpdateTextField integerField = new ImmediateUpdateTextField(definition.getName()) {
                        private static final long serialVersionUID = 1L;

                        protected void save(String text) {
                            saveSetting(definition.getId(), text, obj);
                        };
                    };
                    integerField.setConverter(Integer.class);
                    integerField.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
                    integerField.setRequired(required);
                    integerField.setDescription(description);
                    integerField.setReadOnly(readOnly);
                    formLayout.addComponent(integerField);
                    break;
                case TEXT:
                    ImmediateUpdateTextField textField = new ImmediateUpdateTextField(definition.getName()) {
                        private static final long serialVersionUID = 1L;

                        protected void save(String text) {
                            saveSetting(definition.getId(), text, obj);
                        };
                    };
                    textField.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
                    textField.setRequired(required);
                    textField.setDescription(description);
                    textField.setReadOnly(readOnly);
                    formLayout.addComponent(textField);
                    break;
                case SOURCE_STEP:
                    step = getSingleFlowStep();
                    if (step != null) {
                        Flow flow = context.getConfigurationService().findFlow(step.getFlowId());
                        final AbstractSelect sourceStepsCombo = new ComboBox(definition.getName());
                        sourceStepsCombo.setImmediate(true);

                        List<FlowStepLink> sourceSteps = flow.findFlowStepLinksWithTarget(step.getId());
                        for (FlowStepLink flowStepLink : sourceSteps) {
                            FlowStep sourceStep = flow.findFlowStepWithId(flowStepLink.getSourceStepId());
                            sourceStepsCombo.addItem(sourceStep.getId());
                            sourceStepsCombo.setItemCaption(sourceStep.getId(), sourceStep.getName());
                        }
                        sourceStepsCombo.setValue(obj.get(definition.getId()));
                        sourceStepsCombo.setDescription(description);
                        sourceStepsCombo.setNullSelectionAllowed(false);
                        sourceStepsCombo.setRequired(definition.isRequired());
                        sourceStepsCombo.addValueChangeListener(new ValueChangeListener() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent event) {
                                saveSetting(definition.getId(), (String) sourceStepsCombo.getValue(), obj);
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
                        List<FlowName> flows = context.getConfigurationService().findFlowsInProject(projectVersionId, false);
                        final AbstractSelect combo = new ComboBox(definition.getName());
                        combo.setImmediate(true);
                        for (FlowName name : flows) {
                            if (!step.getFlowId().equals(name.getId())) {
                                combo.addItem(name.getId());
                                combo.setItemCaption(name.getId(), name.getName());
                            }
                        }
                        combo.setValue(obj.get(definition.getId()));
                        combo.setDescription(description);
                        combo.setNullSelectionAllowed(false);
                        combo.setRequired(definition.isRequired());
                        combo.addValueChangeListener(new ValueChangeListener() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent event) {
                                saveSetting(definition.getId(), (String) combo.getValue(), obj);
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
                case ENTITY_COLUMN:
                    if (component != null) {
                        List<ModelEntity> entities = new ArrayList<ModelEntity>();
                        Model model = component.getInputModel();
                        if (model != null) {
                            model.sortAttributes();
                            entities.addAll(model.getModelEntities());
                        }
                        model = component.getOutputModel();
                        if (model != null) {
                            model.sortAttributes();
                            entities.addAll(model.getModelEntities());
                        }
                        AbstractObjectNameBasedSorter.sort(entities);

                        final AbstractSelect entityColumnCombo = new ComboBox(definition.getName());
                        entityColumnCombo.setImmediate(true);

                        for (ModelEntity modelEntity : entities) {
                            for (ModelAttrib attribute : modelEntity.getModelAttributes()) {
                                entityColumnCombo.addItem(attribute.getId());
                                entityColumnCombo.setItemCaption(attribute.getId(), modelEntity.getName() + "." + attribute.getName());
                            }
                        }
                        String currentValue = obj.get(definition.getId());
                        if (currentValue != null) {
                            entityColumnCombo.setValue(obj.get(definition.getId()));
                        }
                        entityColumnCombo.setDescription(description);
                        entityColumnCombo.setNullSelectionAllowed(definition.isRequired());
                        entityColumnCombo.setRequired(definition.isRequired());
                        entityColumnCombo.addValueChangeListener(new ValueChangeListener() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void valueChange(ValueChangeEvent event) {
                                saveSetting(definition.getId(), (String) entityColumnCombo.getValue(), obj);
                            }
                        });
                        entityColumnCombo.setReadOnly(readOnly);
                        formLayout.addComponent(entityColumnCombo);
                    }
                    break;
                case SCRIPT:
                    final AceEditor editor = CommonUiUtils.createAceEditor();
                    editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
                    editor.setTextChangeTimeout(200);
                    editor.setMode(AceMode.java);
                    editor.setHeight(10, Unit.EM);
                    editor.setCaption(definition.getName());
                    editor.setShowGutter(false);
                    editor.setShowPrintMargin(false);
                    editor.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
                    editor.addTextChangeListener(new TextChangeListener() {
                        @Override
                        public void textChange(TextChangeEvent event) {
                            Setting data = obj.findSetting(definition.getId());
                            data.setValue(event.getText());
                            context.getConfigurationService().save(data);
                        }
                    });
                    editor.setReadOnly(readOnly);
                    formLayout.addComponent(editor);
                    break;
                case MULTILINE_TEXT:
                case XML:
                    ImmediateUpdateTextArea area = new ImmediateUpdateTextArea(definition.getName()) {
                        private static final long serialVersionUID = 1L;

                        protected void save(String text) {
                            saveSetting(definition.getId(), text, obj);
                        };
                    };
                    area.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
                    area.setRows(5);
                    area.setRequired(required);
                    area.setDescription(description);
                    area.setReadOnly(readOnly);
                    formLayout.addComponent(area);
                    break;
                default:
                    break;

            }
        }
    }

    protected AbstractSelect createResourceCombo(XMLSetting definition,
            AbstractObjectWithSettings obj, ResourceCategory category) {
        IConfigurationService configurationService = context.getConfigurationService();
        FlowStep step = getSingleFlowStep();
        String projectVersionId = step.getComponent().getProjectVersionId();
        final AbstractSelect combo = new ComboBox(definition.getName());
        combo.setImmediate(true);
        combo.setDescription(definition.getDescription());
        combo.setNullSelectionAllowed(false);
        combo.setRequired(definition.isRequired());
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
                for (Resource resource : resources) {
                    combo.addItem(resource.getId());
                    combo.setItemCaption(resource.getId(), resource.getName());
                }

                combo.setValue(obj.get(definition.getId()));
            }
        }
        combo.addValueChangeListener(
                event -> saveSetting(definition.getId(), (String) combo.getValue(), obj));
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
