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

import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;
import org.jumpmind.metl.core.runtime.component.Transformer;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ExportDialog;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class EditTransformerPanel extends AbstractComponentEditPanel {

    Table table = new Table();

    Table exportTable = new Table();

    TextField filterField;

    AbstractSelect filterPopField;

    List<ComponentAttribSetting> componentAttributes;

    BeanItemContainer<ComponentAttribSetting> container = new BeanItemContainer<ComponentAttribSetting>(
            ComponentAttribSetting.class);
    BeanItemContainer<Record> exportContainer = new BeanItemContainer<Record>(Record.class);

    static final String SHOW_ALL = "Show All";
    static final String SHOW_POPULATED_ENTITIES = "Show Entities with Transforms";
    static final String SHOW_POPULATED_ATTRIBUTES = "Show Attributes with Transforms";

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        filterPopField = new ComboBox();
        filterPopField.addItem(SHOW_ALL);
        filterPopField.addItem(SHOW_POPULATED_ENTITIES);
        filterPopField.addItem(SHOW_POPULATED_ATTRIBUTES);
        if (component.getInputModel() != null) {
            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                filterPopField.addItem(entity.getName());
            }
        }
        filterPopField.setNullSelectionAllowed(false);
        filterPopField.setImmediate(true);
        filterPopField.setWidth(20, Unit.EM);
        filterPopField.setValue(SHOW_ALL);
        filterPopField.addValueChangeListener(event ->  {
            if (isNotBlank(filterField.getValue())) {
                filterField.clear();
            }
            updateTable();
        });
        buttonBar.addLeft(filterPopField);

        buttonBar.addButtonRight("Export", FontAwesome.DOWNLOAD, (e) -> export());

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(event -> {
            String text = event.getText();
            filterPopField.setValue(SHOW_ALL);
            updateTable(text);
        });

        addComponent(buttonBar);

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSortEnabled(true);
        table.setSizeFull();
        table.addGeneratedColumn("entityName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                ComponentAttribSetting setting = (ComponentAttribSetting) itemId;
                Model model = component.getInputModel();
                ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                return UiUtils.getName(filterField.getValue(), entity.getName());
            }
        });
        table.addGeneratedColumn("attributeName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                ComponentAttribSetting setting = (ComponentAttribSetting) itemId;
                Model model = component.getInputModel();
                ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
                return UiUtils.getName(filterField.getValue(), attribute.getName());
            }
        });

        table.addGeneratedColumn("editButton", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                ComponentAttribSetting setting = (ComponentAttribSetting) itemId;
                Button button = new Button();
                button.setIcon(FontAwesome.GEAR);
                button.addClickListener((event) -> new EditTransformWindow(setting).showAtSize(.75));
                return button;
            }
        });       
        
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "value", "editButton" });
        table.setColumnWidth("entityName", 250);
        table.setColumnWidth("attributeName", 250);
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Transform", "Edit" });
        table.setColumnExpandRatio("value", 1);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        addComponent(table);
        setExpandRatio(table, 1.0f);
        

        if (component.getInputModel() != null) {

            componentAttributes = component.getAttributeSettings();
            removeDeadAttributeSettings();

            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    boolean found = false;
                    for (ComponentAttribSetting componentAttribute : componentAttributes) {
                        if (componentAttribute.getAttributeId().equals(attr.getId())
                                && componentAttribute.getName().equals(Transformer.TRANSFORM_EXPRESSION)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found && !attr.getDataType().equals(DataType.REF)
                    		& !attr.getDataType().equals(DataType.ARRAY)) {
                        componentAttributes
                                .add(new ComponentAttribSetting(attr.getId(), component.getId(), Transformer.TRANSFORM_EXPRESSION, null));
                    }
                }
            }

            Collections.sort(componentAttributes, new Comparator<ComponentAttribSetting>() {
                @Override
                public int compare(ComponentAttribSetting o1, ComponentAttribSetting o2) {
                    Model model = component.getInputModel();
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

        updateTable(null);

        exportTable.setContainerDataSource(exportContainer);
        exportTable.setVisibleColumns(new Object[] { "entityName", "attributeName", "value" });
        exportTable.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Transform" });
    }
    
    protected void removeDeadAttributeSettings() {
        List<ComponentAttribSetting> toRemove = new ArrayList<ComponentAttribSetting>();
        for (ComponentAttribSetting componentAttribute : componentAttributes) {
            Model model = component.getInputModel();
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
    
    protected void updateTable() {
        String filter = null;
        if (SHOW_ALL.equals(filterPopField.getValue())) {
            filter = filterField.getValue();
        } 
        updateTable(filter);
    }

    protected void updateTable(String filter) {
        boolean showPopulatedEntities = filterPopField.getValue().equals(SHOW_POPULATED_ENTITIES);
        boolean showPopulatedAttributes = filterPopField.getValue().equals(SHOW_POPULATED_ATTRIBUTES);
        if (!showPopulatedEntities && !showPopulatedAttributes && !filterPopField.getValue().equals(SHOW_ALL)) {
            filter = (String)filterPopField.getValue();
        }

        if (componentAttributes != null) {
            Model model = component.getInputModel();
            Collection<String> entityNames = new ArrayList<>();

            filter = filter != null ? filter.toLowerCase() : null;
            if (model != null) {
                table.removeAllItems();
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
                            table.addItem(componentAttribute);
                        }
                    }
                }
            }
        }
    }

    protected void export() {
        exportTable.removeAllItems();
        updateExportTable(filterField.getValue());
        String fileNamePrefix = component.getName().toLowerCase().replace(' ', '-');
        ExportDialog dialog = new ExportDialog(exportTable, fileNamePrefix, component.getName());
        UI.getCurrent().addWindow(dialog);
    }

    protected void updateExportTable(String filter) {
        boolean showPopulatedEntities = filterPopField.getValue().equals(SHOW_POPULATED_ENTITIES);
        boolean showPopulatedAttributes = filterPopField.getValue().equals(SHOW_POPULATED_ATTRIBUTES);

        if (componentAttributes != null) {
            Model model = component.getInputModel();
            Collection<String> entityNames = new ArrayList<>();

            filter = filter != null ? filter.toLowerCase() : null;
            if (model != null) {
                exportTable.removeAllItems();
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
                            exportTable.addItem(new Record(entity, attribute));
                        }
                    }
                }
            }
        }
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
    
    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final ComponentAttribSetting setting = (ComponentAttribSetting) itemId;
            Field<?> field = null;

            if (propertyId.equals("value") && (setting.getValue() == null || !setting.getValue().contains("\n"))) {
                final ComboBox combo = new ComboBox();
                combo.setWidth(100, Unit.PERCENTAGE);
                String[] functions = ModelAttributeScriptHelper.getSignatures();
                for (String function : functions) {
                    combo.addItem(function);
                }
                combo.setPageLength(functions.length > 20 ? 20 : functions.length);
                if (setting.getValue() != null && !combo.getItemIds().contains(setting.getValue())) {
                    combo.addItem(setting.getValue());
                }
                combo.setImmediate(true);
                combo.setNewItemsAllowed(true);
                combo.addValueChangeListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        setting.setValue((String) combo.getValue());
                        context.getConfigurationService().save(setting);
                    }
                });
                field = combo;
            }
            return field;
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
            
            ComboBox combo = new ComboBox();
            combo.setWidth(400, Unit.PIXELS);
            String[] functions = ModelAttributeScriptHelper.getSignatures();
            for (String function : functions) {
                combo.addItem(function);
            }
            combo.setValue(combo.getItemIds().iterator().next());
            combo.setNullSelectionAllowed(false);
            combo.setPageLength(functions.length > 20 ? 20 : functions.length);
            combo.setImmediate(true);
            
            buttonBar.addLeft(combo);

            buttonBar.addButton("Insert", FontAwesome.SIGN_IN,
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
            editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
            editor.setTextChangeTimeout(200);
            editor.setMode(AceMode.java);
            
            editor.addTextChangeListener(new TextChangeListener() {

                @Override
                public void textChange(TextChangeEvent event) {
                    setting.setValue(event.getText());
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
            updateTable();
        }

    }
}
