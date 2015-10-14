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
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.runtime.component.definition.XMLSettingChoices;
import org.jumpmind.metl.core.runtime.flow.StepRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.ui.common.ApplicationContext;
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
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class PropertySheet extends Panel {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    IComponentRuntimeFactory componentFactory;

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

    public void setSource(Object obj) {
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
                List<XMLSetting> settings = buildSettings(obj);
                if (settings != null) {
                    for (XMLSetting definition : settings) {
                        addSettingField(definition, (AbstractObjectWithSettings) obj, formLayout);
                    }
                }
            }

            if (obj instanceof Component) {
                Component component = (Component) obj;
                XMLComponent componentDefintion = componentFactory.getComonentDefinition(component.getType());
                addThreadCount(componentDefintion, formLayout, component);
                addComponentShared(formLayout, component);      
            }

        }
        setContent(formLayout);
    }

    protected void addComponentProperties(FormLayout formLayout, Component component) {
        XMLComponent componentDefintion = componentFactory.getComonentDefinition(component.getType());
        addComponentName(formLayout, component);
        TextField textField = new TextField("Component Type");
        textField.setValue(component.getType());
        textField.setReadOnly(true);
        formLayout.addComponent(textField);
        addResourceCombo(componentDefintion, formLayout, component);
        addInputModelCombo(componentDefintion, formLayout, component);
        addOutputModelCombo(componentDefintion, formLayout, component);
        addUnitOfWorkCombo(componentDefintion, formLayout, component);
    }
    
    protected void addUnitOfWorkCombo(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        XMLSetting setting = new XMLSetting(StepRuntime.UNIT_OF_WORK, "Unit Of Work", StepRuntime.UNIT_OF_WORK_FLOW, Type.CHOICE, true);
        setting.setChoices(new XMLSettingChoices(StepRuntime.UNIT_OF_WORK_FLOW, StepRuntime.UNIT_OF_WORK_INPUT_MESSAGE));
        addSettingField(setting, component, formLayout);
    }
    
    protected void addThreadCount(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        if (componentDefintion.isSupportsMultipleThreads()) {
            XMLSetting setting = new XMLSetting(StepRuntime.THREAD_COUNT, "Thread Count", "1", Type.INTEGER, true);
            addSettingField(setting, component, formLayout);
        }
    }

    protected void addOutputModelCombo(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        if (value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            String projectVersionId = step.getComponent().getProjectVersionId();
            if ((componentDefintion.getOutputMessageType() == MessageType.ENTITY || componentDefintion.getOutputMessageType() == MessageType.ANY)
                    && !componentDefintion.isInputOutputModelsMatch()) {
                final AbstractSelect combo = new ComboBox("Output Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<ModelName> models = configurationService.findModelsInProject(projectVersionId);
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

                formLayout.addComponent(combo);
            }
        }
    }

    protected void addComponentName(FormLayout formLayout, final Component component) {

        ImmediateUpdateTextField textField = new ImmediateUpdateTextField("Component Name") {
            private static final long serialVersionUID = 1L;

            protected void save(String text) {
                component.setName(text);
                configurationService.save(component);
                if (listener != null) {
                    listener.componentNameChanged(component);
                }
            };
        };
        textField.setValue(component.getName());
        textField.setRequired(true);
        textField.setDescription("Name for the component on the manipulatedFlow");
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
                // than 1 manipulatedFlow?
                // TODO: Refresh palette for the existing manipulatedFlow to have this item
                // display in shared definitions
                component.setShared((boolean) event.getProperty().getValue());
                configurationService.save(component);
            }
        });
        formLayout.addComponent(checkBox);

    }
    
    protected void addInputModelCombo(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        if (value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            String projectVersionId = step.getComponent().getProjectVersionId();
            if (componentDefintion.getInputMessageType() == MessageType.ENTITY || componentDefintion.getInputMessageType() == MessageType.ANY) {
                final AbstractSelect combo = new ComboBox("Input Model");
                combo.setImmediate(true);
                combo.setNullSelectionAllowed(true);
                List<ModelName> models = configurationService.findModelsInProject(projectVersionId);
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
                        configurationService.save((AbstractObject) component);
                        setSource(value);
                    }
                });

                formLayout.addComponent(combo);
            }
        }
    }

    protected void addResourceCombo(XMLComponent componentDefintion, FormLayout formLayout, final Component component) {
        if (componentDefintion.getResourceCategory() != null && componentDefintion.getResourceCategory() != ResourceCategory.NONE
                && value instanceof FlowStep) {
            FlowStep step = (FlowStep) value;
            final AbstractSelect resourcesCombo = new ComboBox("Resource");
            resourcesCombo.setImmediate(true);
            resourcesCombo.setRequired(true);
            List<String> types = resourceFactory.getResourceTypes(componentDefintion.getResourceCategory());
            String projectVersionId = step.getComponent().getProjectVersionId();
            if (types != null) {
                List<Resource> resources = configurationService.findResourcesByTypes(projectVersionId,
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
                    configurationService.save(component);
                }
            });

            formLayout.addComponent(resourcesCombo);
        }
    }

    protected List<XMLSetting> buildSettings(Object obj) {
        if (obj instanceof Component) {
            Component component = (Component) obj;
            XMLComponent definition = componentFactory.getComonentDefinition(component.getType());
            return definition.getSettings().getSetting();
        } else if (obj instanceof Resource) {
            Resource resource = (Resource) obj;
            List<XMLSetting> xmlSettings = new ArrayList<XMLSetting>();
            Map<String, SettingDefinition> resourceSettings = resourceFactory.getSettingDefinitionsForResourceType(resource.getType());
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
                component = (Component)obj;
            }
            String description = definition.getDescription();
            Type type = definition.getType();
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
                        }
                    });
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
                    formLayout.addComponent(textField);
                    break;
                case SOURCE_STEP:
                    if (value instanceof FlowStep) {
                        FlowStep step = (FlowStep) value;
                        Flow flow = configurationService.findFlow(step.getFlowId());
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
                        formLayout.addComponent(sourceStepsCombo);
                    }
                    break;
                case FLOW:
                    if (value instanceof FlowStep) {
                        FlowStep step = (FlowStep) value;
                        String projectVersionId = step.getComponent().getProjectVersionId();
                        List<FlowName> flows = configurationService.findFlowsInProject(projectVersionId);
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
                        formLayout.addComponent(combo);
                    }
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
                        String currentValue  = obj.get(definition.getId());
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
                            configurationService.save(data);
                        }
                    });
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
                    formLayout.addComponent(area);
                    break;
                default:
                    break;

            }
        }

    }

    protected void saveSetting(String key, String text, AbstractObjectWithSettings obj) {
        Setting data = obj.findSetting(key);
        data.setValue(text);
        configurationService.save(data);
    }

}
