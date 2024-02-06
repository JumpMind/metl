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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;
import org.jumpmind.metl.core.runtime.component.Transformer;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ResizableDialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.events.AceValueChanged;

@SuppressWarnings("serial")
public class EditTransformerPanel extends AbstractComponentEditPanel {

    Grid<ComponentAttribSetting> grid = new Grid<ComponentAttribSetting>();

    Grid<Record> exportGrid = new Grid<Record>();

    TextField filterField;

    ComboBox<String> filterPopField;

    List<ComponentAttribSetting> componentAttributes;

    static final String SHOW_ALL = "Show All";
    static final String SHOW_POPULATED_ENTITIES = "Show Entities with Transforms";
    static final String SHOW_POPULATED_ATTRIBUTES = "Show Attributes with Transforms";

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);

        filterPopField = new ComboBox<String>();
        List<String> itemList = new ArrayList<String>();
        itemList.add(SHOW_ALL);
        itemList.add(SHOW_POPULATED_ENTITIES);
        itemList.add(SHOW_POPULATED_ATTRIBUTES);
        if (component.getInputModel() != null) {
            for (ModelEntity entity : ((RelationalModel)component.getInputModel()).getModelEntities()) {
            	itemList.add(entity.getName());
            }
        }
        filterPopField.setItems(itemList);
        filterPopField.setWidth("20em");
        filterPopField.setValue(SHOW_ALL);
        filterPopField.addValueChangeListener(event ->  {
            if (event.getValue() != null) {
                if (isNotBlank(filterField.getValue())) {
                    filterField.clear();
                }
                updateGrid();
            } else {
                filterPopField.setValue(event.getOldValue());
            }
        });
        buttonBar.addLeft(filterPopField);

        buttonBar.addButtonRight("Export", VaadinIcon.DOWNLOAD, (e) -> export());

        filterField = buttonBar.addFilter();
        filterField.addValueChangeListener(event -> {
            String text = event.getValue();
            filterPopField.setValue(SHOW_ALL);
            updateGrid(text);
        });

        add(buttonBar);

        if (component.getInputModel() != null) {

            componentAttributes = component.getAttributeSettings();
            removeDeadAttributeSettings();

            for (ModelEntity entity : ((RelationalModel)component.getInputModel()).getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    boolean found = false;
                    for (ComponentAttribSetting componentAttribute : componentAttributes) {
                        if (componentAttribute.getAttributeId().equals(attr.getId())
                                && componentAttribute.getName().equals(Transformer.TRANSFORM_EXPRESSION)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        componentAttributes
                                .add(new ComponentAttribSetting(attr.getId(), component.getId(), Transformer.TRANSFORM_EXPRESSION, null));
                    }
                }
            }

            Collections.sort(componentAttributes, new Comparator<ComponentAttribSetting>() {
                @Override
                public int compare(ComponentAttribSetting o1, ComponentAttribSetting o2) {
                    RelationalModel model = (RelationalModel) component.getInputModel();
                    ModelAttrib attribute1 = model.getAttributeById(o1.getAttributeId());
                    ModelEntity entity1 = model.getEntityById(attribute1.getEntityId());

                    ModelAttrib attribute2 = model.getAttributeById(o2.getAttributeId());
                    ModelEntity entity2 = model.getEntityById(attribute2.getEntityId());

                    int compare = entity1.getName().compareTo(entity2.getName());
                    if (compare == 0) {
                        compare = attribute1.getName().compareTo(attribute2.getName());
                    }
                    return compare;
                }
            });
        }

        grid.setSizeFull();
        grid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
            ModelEntity entity = model.getEntityById(attribute.getEntityId());
            return UiUtils.getName(filterField.getValue(), entity.getName());
        }).setHeader("Entity Name").setWidth("250px").setSortable(true);
        grid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
            return UiUtils.getName(filterField.getValue(), attribute.getName());
        }).setHeader("Attribute Name").setWidth("250px").setSortable(true);
        final ComboBox<String> combo = new ComboBox<String>();
        combo.setWidthFull();
        List<String> functionList = new ArrayList<String>();
        String[] functions = ModelAttributeScriptHelper.getSignatures();
        for (String function : functions) {
            functionList.add(function);
        }
        combo.setPageSize(functions.length > 20 ? 20 : functions.length);
        for (ComponentAttribSetting setting : componentAttributes) {
            if (setting.getValue() != null && !functionList.contains(setting.getValue())) {
                functionList.add(setting.getValue());
            }
        }
        combo.setItems(functionList);
        combo.setAllowCustomValue(true);
        combo.addCustomValueSetListener(event -> {
            functionList.add(event.getDetail());
            combo.setItems(functionList);
            combo.setValue(event.getDetail());
        });
        Editor<ComponentAttribSetting> editor = grid.getEditor();
        Binder<ComponentAttribSetting> binder = new Binder<ComponentAttribSetting>();
        editor.setBinder(binder);
        binder.forField(combo).bind(ComponentAttribSetting::getValue, ComponentAttribSetting::setValue);
        grid.addColumn(ComponentAttribSetting::getValue).setEditorComponent(combo)
                .setHeader("Transform").setFlexGrow(1).setSortable(true);
        grid.addComponentColumn(setting -> {
            Button button = new Button();
            button.setIcon(new Icon(VaadinIcon.COG));
            button.addClickListener((event) -> new EditTransformDialog(setting).showAtSize(.75));
            return button;
        }).setHeader("Edit").setSortable(false);
        
        editor.addSaveListener(event -> context.getConfigurationService().save(event.getItem()));
        grid.addItemDoubleClickListener(event -> editor.editItem(event.getItem()));
        add(grid);
        expand(grid);

        updateGrid(null);

        exportGrid.addColumn(Record::getEntityName).setHeader("Entity Name");
        exportGrid.addColumn(Record::getAttributeName).setHeader("Attribute Name");
        exportGrid.addColumn(Record::getValue).setHeader("Transform");
    }
    
    protected void removeDeadAttributeSettings() {
        List<ComponentAttribSetting> toRemove = new ArrayList<ComponentAttribSetting>();
        for (ComponentAttribSetting componentAttribute : componentAttributes) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute1 = model.getAttributeById(componentAttribute.getAttributeId());
            if (attribute1 == null) {
                /*
                 * invalid attribute. model must have changed. lets remove
                 * it
                 */
                toRemove.add(componentAttribute);
            }
        }

        for (ComponentAttribSetting componentAttributeSetting : toRemove) {
            componentAttributes.remove(componentAttributeSetting);
            context.getConfigurationService().delete(componentAttributeSetting);
        }
    }
    
    protected void updateGrid() {
        String filter = null;
        if (SHOW_ALL.equals(filterPopField.getValue())) {
            filter = filterField.getValue();
        } 
        updateGrid(filter);
    }

    protected void updateGrid(String filter) {
        boolean showPopulatedEntities = filterPopField.getValue().equals(SHOW_POPULATED_ENTITIES);
        boolean showPopulatedAttributes = filterPopField.getValue().equals(SHOW_POPULATED_ATTRIBUTES);
        if (!showPopulatedEntities && !showPopulatedAttributes && !filterPopField.getValue().equals(SHOW_ALL)) {
            filter = (String)filterPopField.getValue();
        }

        if (componentAttributes != null) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            Collection<String> entityNames = new ArrayList<>();

            filter = filter != null ? filter.toLowerCase() : null;
            List<ComponentAttribSetting> filteredComponentAttributes = new ArrayList<ComponentAttribSetting>();
            if (model != null) {
                // loop through the attributes with transforms to get a list of
                // entities
                for (ComponentAttribSetting componentAttribute : componentAttributes) {
                    ModelAttrib attribute = model.getAttributeById(componentAttribute.getAttributeId());
                    ModelEntity entity = model.getEntityById(attribute.getEntityId());
                    if (isNotBlank(componentAttribute.getValue()) && !entityNames.contains(entity.getName())) {
                        entityNames.add(entity.getName());
                    }
                }

                for (ComponentAttribSetting componentAttribute : componentAttributes) {
                    ModelAttrib attribute = model.getAttributeById(componentAttribute.getAttributeId());
                    ModelEntity entity = model.getEntityById(attribute.getEntityId());

                    boolean populated = (showPopulatedEntities && entityNames.contains(entity.getName()))
                            || (showPopulatedAttributes && isNotBlank(componentAttribute.getValue()))
                            || (!showPopulatedAttributes && !showPopulatedEntities);
                    if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)
                            || attribute.getName().toLowerCase().contains(filter)) {
                        if (populated) {
                            filteredComponentAttributes.add(componentAttribute);
                        }
                    }
                }
            }
            grid.setItems(filteredComponentAttributes);
        }
    }

    protected void export() {
        updateExportGrid(filterField.getValue());
        ExportDialog.show(context, exportGrid);
    }

    protected void updateExportGrid(String filter) {
        boolean showPopulatedEntities = filterPopField.getValue().equals(SHOW_POPULATED_ENTITIES);
        boolean showPopulatedAttributes = filterPopField.getValue().equals(SHOW_POPULATED_ATTRIBUTES);

        List<Record> recordList = new ArrayList<Record>();
        if (componentAttributes != null) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            Collection<String> entityNames = new ArrayList<>();

            filter = filter != null ? filter.toLowerCase() : null;
            if (model != null) {
                // loop through the attributes with transforms to get a list of
                // entities
                for (ComponentAttribSetting componentAttribute : componentAttributes) {
                    ModelAttrib attribute = model.getAttributeById(componentAttribute.getAttributeId());
                    ModelEntity entity = model.getEntityById(attribute.getEntityId());
                    if (isNotBlank(componentAttribute.getValue()) && !entityNames.contains(entity.getName())) {
                        entityNames.add(entity.getName());
                    }
                }

                for (ComponentAttribSetting componentAttribute : componentAttributes) {
                    ModelAttrib attribute = model.getAttributeById(componentAttribute.getAttributeId());
                    ModelEntity entity = model.getEntityById(attribute.getEntityId());

                    boolean populated = (showPopulatedEntities && entityNames.contains(entity.getName()))
                            || (showPopulatedAttributes && isNotBlank(componentAttribute.getValue()))
                            || (!showPopulatedAttributes && !showPopulatedEntities);
                    if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)
                            || attribute.getName().toLowerCase().contains(filter)) {
                        if (populated) {
                            recordList.add(new Record(entity, attribute));
                        }
                    }
                }
            }
        }
        exportGrid.setItems(recordList);
    }

    public class Record {
        ModelEntity modelEntity;

        ModelAttrib modelAttribute;

        String entityName = "";

        String attributeName = "";

        String value = "";

        public Record(ModelEntity modelEntity, ModelAttrib modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;

            if (modelEntity != null) {
                this.entityName = modelEntity.getName();
            }

            if (modelAttribute != null) {
                this.attributeName = modelAttribute.getName();
                ComponentAttribSetting setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                        Transformer.TRANSFORM_EXPRESSION);
                if (setting != null) {
                    this.value = setting.getValue();
                }
            }
        }

        public int hashCode() {
            return modelEntity.hashCode() + modelAttribute.hashCode();
        }

        public String getEntityName() {
            return modelEntity.getName();
        }

        public String getAttributeName() {
            return modelAttribute.getName();
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    
    class EditTransformDialog extends ResizableDialog {
        private static final long serialVersionUID = 1L;
        
        AceEditor editor;
        
        int cursorPosition = 0;

        public EditTransformDialog(ComponentAttribSetting setting) {
            super("Transform");
            setWidth("800px");
            setHeight("600px");
            innerContent.setMargin(true);
            
            ButtonBar buttonBar = new ButtonBar();
            add(buttonBar);
            
            ComboBox<String> combo = new ComboBox<String>();
            combo.setWidth("400px");
            String[] functions = ModelAttributeScriptHelper.getSignatures();
            combo.setItems(functions);
            if (functions.length > 0) {
            	combo.setValue(functions[0]);
            }
            combo.addValueChangeListener(event -> {
                if (event.getValue() == null) {
                    combo.setValue(event.getOldValue());
                }
            });
            combo.setPageSize(functions.length > 20 ? 20 : functions.length);
            
            buttonBar.addLeft(combo);

            buttonBar.addButton("Insert", VaadinIcon.SIGN_IN,
                    new ComponentEventListener<ClickEvent<Button>>() {
                            
                        @Override
                        public void onComponentEvent(ClickEvent<Button> event) {
                            String script  = (editor.getValue()==null) ? "" : editor.getValue();
                            StringBuilder builder = new StringBuilder(script);
                            String substring = (String) combo.getValue();
                            builder.insert(cursorPosition, substring);
                            editor.setValue(builder.toString());
                            editor.setSelection(cursorPosition, cursorPosition + substring.length());
                            // Manually save text since TextChangeListener is not firing.
                            setting.setValue(editor.getValue());
                            EditTransformerPanel.this.context.getConfigurationService()
                                    .save(setting);
                        }
                    });
            
            
            editor = CommonUiUtils.createAceEditor();
            editor.setMode(AceMode.java);
            
            editor.addSelectionChangeListener(event -> cursorPosition = event.getCursorPosition().getIndex());
            editor.addValueChangeListener(new ComponentEventListener<AceValueChanged>() {

                @Override
                public void onComponentEvent(AceValueChanged event) {
                    setting.setValue(event.getValue());
                    EditTransformerPanel.this.context.getConfigurationService()
                            .save(setting);
                }
            });
            editor.setValue(setting.getValue());
            
            innerContent.add(editor);
            innerContent.expand(editor);
            
            add(buildButtonFooter(buildCloseButton()));
            
        }
        
        @Override
        public void close() {
            super.close();
            updateGrid();
        }

    }
}
