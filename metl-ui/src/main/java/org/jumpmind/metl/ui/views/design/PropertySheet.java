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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.AbstractObjectWithSettings;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.runtime.component.definition.XMLSettingChoices;
import org.jumpmind.metl.core.runtime.flow.StepRuntime;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.definition.XMLComponentUI;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ImmediateUpdatePasswordField;
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
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class PropertySheet extends AbsoluteLayout {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    IPropertySheetChangeListener listener;

    Object value;

    Panel panel;

    Button editButton;

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

        editButton = new Button("Component Editor");
        editButton.addClickListener(event -> openAdvancedEditor());
        addComponent(editButton, "right: 25px; top: 10px;");

    }

    protected boolean hasAdvancedEditor() {
        FlowStep flowStep = getSingleFlowStep();
        if (flowStep != null) {
            String type = flowStep.getComponent().getType();
            XMLComponentUI definition = context.getUiFactory().getDefinition(type);
            return definition != null && definition.getClassName() != null;
        } else {
            return false;
        }
    }

    public void openAdvancedEditor() {
        FlowStep flowStep = getSingleFlowStep();
        if (flowStep != null) {
            String type = flowStep.getComponent().getType();
            IComponentEditPanel panel = context.getUiFactory().create(type);
            if (panel != null) {
                if (panel instanceof IFlowStepAware) {
                    Flow flow = context.getConfigurationService().findFlow(flowStep.getFlowId());
                    ((IFlowStepAware) panel).makeAwareOf(flowStep, flow);
                }
                panel.init(readOnly, flowStep.getComponent(), context, this);
                tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel);
            }
        }
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
        editButton.setVisible(hasAdvancedEditor());
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
                XMLComponent componentDefintion = context.getComponentDefinitionFactory().getDefinition(component.getType());
                addThreadCount(componentDefintion, formLayout, component);
                addComponentShared(formLayout, component);
            }

            if (obj instanceof List<?>) {
                addCommonComponentSettings(formLayout, obj);
            }

        }
        panel.setContent(formLayout);
    }

    @SuppressWarnings("unchecked")
    protected void addCommonComponentSettings(FormLayout formLayout, Object obj) {
        List<Object> list = (List<Object>) obj;
        List<Component> components = new ArrayList<Component>(list.size());

        // Check if all selected components support the enabled property
        // TODO: Support more than the enable component.
        // Look for all common parameters.
        boolean supportEnable = true;
        boolean enabled = true;
        for (Object o : list) {
            if (o instanceof FlowStep) {
                Component component = ((FlowStep) o).getComponent();
                if (!hasSetting(component, AbstractComponentRuntime.ENABLED)) {
                    supportEnable = false;
                    break;
                }
                if (enabled && !component.getBoolean(AbstractComponentRuntime.ENABLED, true)) {
                    enabled = false;
                }
                components.add(component);
            } else {
                supportEnable = false;
                break;
            }
        }

        // Create the enabled field if all selected components support the
        // enabled setting.
        if (components.size() != 0 && supportEnable) {
            final CheckBox checkBox = new CheckBox("Enabled");
            checkBox.setImmediate(true);
            checkBox.setRequired(true);
            checkBox.setValue(enabled);
            checkBox.addValueChangeListener((event) -> {
                for (final Component component : components) {
                    saveSetting(AbstractComponentRuntime.ENABLED, checkBox.getValue().toString(), component);
                }
                if (listener != null) {
                    listener.componentChanged(components);
                }
            });
            checkBox.setReadOnly(readOnly);
            formLayout.addComponent(checkBox);
        }
    }

    private boolean hasSetting(Component component, String setting) {
        XMLComponent componentDefinition = context.getComponentDefinitionFactory().getDefinition(component.getType());
        return (componentDefinition.findXMLSetting(setting) != null);
    }

    protected void addResourceProperties(FormLayout formLayout, Resource resource) {
        TextField textField = new TextField("Resource Type");
        textField.setValue(resource.getType());
        textField.setReadOnly(true);
        formLayout.addComponent(textField);
    }

    protected void addComponentProperties(FormLayout formLayout, Component component) {
        XMLComponent componentDefintion = context.getComponentDefinitionFactory().getDefinition(component.getType());
        addComponentName(formLayout, component);
        TextField textField = new TextField("Component Type");
        textField.setValue(componentDefintion.getName());
        textField.setReadOnly(true);
        formLayout.addComponent(textField);
        addResourceCombo(componentDefintion, formLayout, component);
        addInputModelCombo(componentDefintion, formLayout, component);
        addOutputModelCombo(componentDefintion, formLayout, component);
    }

    protected void addThreadCount(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        if (componentDefintion.isSupportsMultipleThreads()) {
            XMLSetting setting = new XMLSetting(StepRuntime.THREAD_COUNT, "Thread Count", "1", Type.INTEGER, true);
            addSettingField(setting, component, formLayout);
        }
    }

    protected void addOutputModelCombo(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        FlowStep step = getSingleFlowStep();
        if (step != null) {
            String projectVersionId = step.getComponent().getProjectVersionId();
            if ((componentDefintion.getOutputMessageType() == MessageType.ENTITY
                    || componentDefintion.getOutputMessageType() == MessageType.ANY) && !componentDefintion.isInputOutputModelsMatch()) {
                final AbstractSelect combo = new ComboBox("Output Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<ModelName> models = context.getConfigurationService().findModelsInProject(projectVersionId);
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
                            component.setOutputModel(context.getConfigurationService().findModel(model.getId()));
                        } else {
                            component.setOutputModel(null);
                        }
                        context.getConfigurationService().save((AbstractObject) component);
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

    protected void addComponentShared(FormLayout formLayout, final Component component) {

        final CheckBox checkBox = new CheckBox("Shared");
        checkBox.setImmediate(true);

        if (component.isShared()) {
            checkBox.setValue(true);
        } else {
            checkBox.setValue(false);
        }
        checkBox.setRequired(true);
        checkBox.setDescription("Whether this component can be reused");
        checkBox.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                // TODO: Don't allow unshare if component is already on more
                // than 1 flow?
                // TODO: Refresh palette for the existing flow to have this item
                // display in shared definitions
                component.setShared((boolean) event.getProperty().getValue());
                context.getConfigurationService().save(component);
            }
        });
        checkBox.setReadOnly(readOnly);
        formLayout.addComponent(checkBox);

    }

    protected void addInputModelCombo(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        FlowStep step = getSingleFlowStep();
        if (step != null) {
            String projectVersionId = step.getComponent().getProjectVersionId();
            if (componentDefintion.getInputMessageType() == MessageType.ENTITY
                    || componentDefintion.getInputMessageType() == MessageType.ANY) {
                final AbstractSelect combo = new ComboBox("Input Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<ModelName> models = context.getConfigurationService().findModelsInProject(projectVersionId);
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
                            component.setInputModel(context.getConfigurationService().findModel(model.getId()));
                        } else {
                            component.setInputModel(null);
                        }
                        if (componentDefintion.isInputOutputModelsMatch()) {
                            component.setOutputModel(component.getInputModel());
                        }
                        context.getConfigurationService().save((AbstractObject) component);
                        setSource(value);
                    }
                });
                combo.setReadOnly(readOnly);
                formLayout.addComponent(combo);
            }
        }
    }

    protected void addResourceCombo(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        if (componentDefintion == null) {
            log.info("null kaboom " + component.getName() + " " + component.getType());
        }
        FlowStep step = getSingleFlowStep();
        if (componentDefintion.getResourceCategory() != null && componentDefintion.getResourceCategory() != ResourceCategory.NONE
                && step != null) {
            final AbstractSelect resourcesCombo = new ComboBox("Resource");
            resourcesCombo.setImmediate(true);
            List<String> types = context.getResourceFactory().getResourceTypes(componentDefintion.getResourceCategory());
            String projectVersionId = step.getComponent().getProjectVersionId();
            if (types != null) {
                List<Resource> resources = context.getConfigurationService().findResourcesByTypes(projectVersionId,
                        types.toArray(new String[types.size()]));
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

    protected List<XMLSetting> buildSettings(Object obj) {
        if (obj instanceof Component) {
            Component component = (Component) obj;
            XMLComponent definition = context.getComponentDefinitionFactory().getDefinition(component.getType());
            return definition.getSettings().getSetting();
        } else if (obj instanceof Resource) {
            Resource resource = (Resource) obj;
            List<XMLSetting> xmlSettings = new ArrayList<XMLSetting>();
            Map<String, SettingDefinition> resourceSettings = context.getResourceFactory()
                    .getSettingDefinitionsForResourceType(resource.getType());
            for (String key : resourceSettings.keySet()) {
                SettingDefinition def = resourceSettings.get(key);
                XMLSetting setting = new XMLSetting();
                setting.setId(key);
                setting.setName(def.label());
                setting.setDefaultValue(def.defaultValue());
                setting.setRequired(def.required());
                setting.setVisible(def.visible());
                setting.setType(def.type());
                if (def.type() == Type.CHOICE) {
                    XMLSettingChoices choices = new XMLSettingChoices();
                    choices.setChoice(new ArrayList<String>());
                    for (String choice : def.choices()) {
                        choices.getChoice().add(choice);
                    }
                    setting.setChoices(choices);
                }
                xmlSettings.add(setting);
            }
            return xmlSettings;
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
                    ImmediateUpdatePasswordField passwordField = new ImmediateUpdatePasswordField(definition.getName()) {
                        private static final long serialVersionUID = 1L;

                        protected void save(String text) {
                            saveSetting(definition.getId(), text, obj);
                        };
                    };
                    passwordField.setValue(obj.get(definition.getId(), definition.getDefaultValue()));
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
                            for (ModelAttribute attribute : modelEntity.getModelAttributes()) {
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

    protected AbstractSelect createResourceCombo(XMLSetting definition, AbstractObjectWithSettings obj, ResourceCategory category) {
        FlowStep step = getSingleFlowStep();
        String projectVersionId = step.getComponent().getProjectVersionId();
        final AbstractSelect combo = new ComboBox(definition.getName());
        combo.setImmediate(true);
        combo.setDescription(definition.getDescription());
        combo.setNullSelectionAllowed(false);
        List<String> types = context.getResourceFactory().getResourceTypes(category);
        if (types != null) {
            List<Resource> resources = context.getConfigurationService().findResourcesByTypes(projectVersionId,
                    types.toArray(new String[types.size()]));
            if (resources != null) {
                for (Resource resource : resources) {
                    combo.addItem(resource.getId());
                    combo.setItemCaption(resource.getId(), resource.getName());
                }

                combo.setValue(obj.get(definition.getId()));
            }
        }
        combo.addValueChangeListener(event -> saveSetting(definition.getId(), (String) combo.getValue(), obj));
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
