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
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TextField;

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
        addComponent(buttonBar);

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
        filterPopField.setEmptySelectionAllowed(false);
        filterPopField.setWidth(20, Unit.EM);
        filterPopField.setValue(SHOW_ALL);
        filterPopField.addValueChangeListener(event ->  {
            if (isNotBlank(filterField.getValue())) {
                filterField.clear();
            }
            updateGrid();
        });
        buttonBar.addLeft(filterPopField);

        buttonBar.addButtonRight("Export", VaadinIcons.DOWNLOAD, (e) -> export());

        filterField = buttonBar.addFilter();
        filterField.addValueChangeListener(event -> {
            String text = event.getValue();
            filterPopField.setValue(SHOW_ALL);
            updateGrid(text);
        });

        addComponent(buttonBar);

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
        }).setCaption("Entity Name").setWidth(250).setSortable(true);
        grid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
            return UiUtils.getName(filterField.getValue(), attribute.getName());
        }).setCaption("Attribute Name").setWidth(250).setSortable(true);
        final ComboBox<String> combo = new ComboBox<String>();
        combo.setWidth(100, Unit.PERCENTAGE);
        List<String> functionList = new ArrayList<String>();
        String[] functions = ModelAttributeScriptHelper.getSignatures();
        for (String function : functions) {
            functionList.add(function);
        }
        combo.setPageLength(functions.length > 20 ? 20 : functions.length);
        for (ComponentAttribSetting setting : componentAttributes) {
            if (setting.getValue() != null && !functionList.contains(setting.getValue())) {
                functionList.add(setting.getValue());
            }
        }
        combo.setItems(functionList);
        combo.setNewItemProvider(newItem -> {
            functionList.add(newItem);
            combo.setItems(functionList);
            combo.setValue(newItem);
            return Optional.of(newItem);
        });
        grid.addColumn(ComponentAttribSetting::getValue).setEditorComponent(combo, ComponentAttribSetting::setValue)
                .setCaption("Transform").setExpandRatio(1).setSortable(true);
        grid.addComponentColumn(setting -> {
            Button button = new Button();
            button.setIcon(VaadinIcons.COG);
            button.addClickListener((event) -> new EditTransformWindow(setting).showAtSize(.75));
            return button;
        }).setCaption("Edit").setSortable(false);
        
        grid.getEditor().setEnabled(true).addSaveListener(event -> context.getConfigurationService().save(event.getBean()));
        addComponent(grid);
        setExpandRatio(grid, 1.0f);

        updateGrid(null);

        exportGrid.addColumn(Record::getEntityName).setCaption("Entity Name");
        exportGrid.addColumn(Record::getAttributeName).setCaption("Attribute Name");
        exportGrid.addColumn(Record::getValue).setCaption("Transform");
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
    
    class EditTransformWindow extends ResizableWindow {
        private static final long serialVersionUID = 1L;
        
        AceEditor editor;

        public EditTransformWindow(ComponentAttribSetting setting) {
            super("Transform");
            setWidth(800f, Unit.PIXELS);
            setHeight(600f, Unit.PIXELS);
            content.setMargin(true);
            
            ButtonBar buttonBar = new ButtonBar();
            addComponent(buttonBar);
            
            ComboBox<String> combo = new ComboBox<String>();
            combo.setWidth(400, Unit.PIXELS);
            String[] functions = ModelAttributeScriptHelper.getSignatures();
            combo.setItems(functions);
            if (functions.length > 0) {
            	combo.setValue(functions[0]);
            }
            combo.setEmptySelectionAllowed(false);
            combo.setPageLength(functions.length > 20 ? 20 : functions.length);
            
            buttonBar.addLeft(combo);

            buttonBar.addButton("Insert", VaadinIcons.SIGN_IN,
                    new ClickListener() {
                            
                        @Override
                        public void buttonClick(ClickEvent event) {
                            String script  = (editor.getValue()==null) ? "" : editor.getValue();
                            StringBuilder builder = new StringBuilder(script);
                            String substring = (String) combo.getValue();
                            int startPosition = editor.getCursorPosition();
                            builder.insert(startPosition, substring);
                            editor.setValue(builder.toString());
                            editor.setSelection(startPosition, startPosition + substring.length());
                            // Manually save text since TextChangeListener is not firing.
                            setting.setValue(editor.getValue());
                            EditTransformerPanel.this.context.getConfigurationService()
                                    .save(setting);
                        }
                    });
            
            
            editor = CommonUiUtils.createAceEditor();
            editor.setMode(AceMode.java);
            
            editor.addValueChangeListener(new ValueChangeListener<String>() {

                @Override
                public void valueChange(ValueChangeEvent<String> event) {
                    setting.setValue(event.getValue());
                    EditTransformerPanel.this.context.getConfigurationService()
                            .save(setting);
                }
            });
            editor.setValue(setting.getValue());
            
            content.addComponent(editor);
            content.setExpandRatio(editor, 1);
            
            addComponent(buildButtonFooter(buildCloseButton()));
            
        }
        
        @Override
        public void close() {
            super.close();
            updateGrid();
        }

    }
}
