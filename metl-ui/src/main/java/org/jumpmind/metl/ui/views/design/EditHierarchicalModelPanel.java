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

import static org.apache.commons.lang.StringUtils.trim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jumpmind.metl.core.model.HierarchicalDataType;
import org.jumpmind.metl.core.model.HierarchicalModel;
import org.jumpmind.metl.core.model.ModelSchemaObject;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;

import com.vaadin.flow.component.BlurNotifier.BlurEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;

@SuppressWarnings("serial")
public class EditHierarchicalModelPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TreeGrid<ModelSchemaObject> treeGrid = new TreeGrid<ModelSchemaObject>();

    Grid<Record> grid = new Grid<Record>();

    HierarchicalModel model;

    Set<ModelSchemaObject> lastEditItemIds = Collections.emptySet();

    TableColumnSelectDialog tableColumnSelectDialog;

    Button addSchemaObjectButton;

    Button editButton;

    Button removeButton;
    
    ShortcutRegistration enterKeyRegistration;

    boolean readOnly;

    public EditHierarchicalModelPanel(ApplicationContext context, String modelId, boolean readOnly) {
        this.context = context;
        this.model = new HierarchicalModel(modelId);
        this.readOnly = readOnly;
        context.getConfigurationService().refresh(model);
        ButtonBar buttonBar1 = new ButtonBar();
        add(buttonBar1);
        
        addSchemaObjectButton = buttonBar1.addButton("Add SchemaObject", VaadinIcon.PLUS);
        addSchemaObjectButton.addClickListener(new AddSchemaObjectClickListener());
        
        editButton = buttonBar1.addButton("Edit", VaadinIcon.EDIT);
        editButton.addClickListener(new EditClickListener());
        
        removeButton = buttonBar1.addButton("Remove", VaadinIcon.TRASH);
        removeButton.addClickListener(new RemoveClickListener());

        treeGrid.setSizeFull();
        treeGrid.setPageSize(100);
        treeGrid.setSelectionMode(SelectionMode.MULTI);
        treeGrid.addColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                TextField t = new TextField();
                t.setValueChangeMode(ValueChangeMode.LAZY);
                t.setValueChangeTimeout(200);
                t.addValueChangeListener(event -> {
                    String newName = trim(event.getValue());
                    obj.setName(newName);
                    EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);                            
                });
                t.setWidthFull();
                t.setValue(obj.getName());
                t.focus();
                t.getElement().executeJs("this.inputElement.select()");
                t.setPrefixComponent(new Icon(VaadinIcon.CUBE));
                return t;
            } else {
                return new HorizontalLayout(new Icon(VaadinIcon.CUBE), new Span(obj.getName()));
            }
        }).setHeader("Name");

        treeGrid.addColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                TextField t = new TextField();
                t.setValueChangeMode(ValueChangeMode.LAZY);
                t.setValueChangeTimeout(200);
                t.addValueChangeListener(event -> {
                    obj.setDescription(trim(event.getValue()));
                    EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                });
                t.setWidthFull();
                t.setValue(obj.getDescription());
                return t;
            } else {
                return obj.getDescription();
            }
        }).setHeader("Description");

        treeGrid.addColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                final ComboBox<String> cbox = new ComboBox<String>();
                List<String> dataTypeList = new ArrayList<String>();
                for (HierarchicalDataType dataType : HierarchicalDataType.values()) {
                    dataTypeList.add(dataType.name());
                }
                cbox.setItems(dataTypeList);
                cbox.setValue(obj.getType());
                cbox.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
                    public void valueChanged(ValueChangeEvent<String> event) {
                        if (event.getValue() != null) {
                            obj.setType(cbox.getValue());
                            EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                        } else {
                            cbox.setValue(event.getOldValue());
                        }
                    }
                });
                cbox.addBlurListener(new ComponentEventListener<BlurEvent<ComboBox<String>>>() {
                    public void onComponentEvent(BlurEvent<ComboBox<String>> event) {
                        List<ModelSchemaObject> items = getAllItems();
                        boolean found = false;
                        for (ModelSchemaObject item : items) {
                            if (item.equals(obj)) {
                                found = true;
                            } else if (found) {
                                selectOnly(item);
                                editSelectedItem();
                                break;
                            }
                        }
                    }
                });
                return cbox;
            } else {
                return obj.getType();
            }
        }).setHeader("Type");

        treeGrid.addColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                TextField t = new TextField();
                t.setValueChangeMode(ValueChangeMode.LAZY);
                t.setValueChangeTimeout(200);
                t.addValueChangeListener(event -> {
                    obj.setPattern(trim(event.getValue()));
                    EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                });
                t.setWidthFull();
                t.setValue(obj.getPattern());
                return t;
            } else {
                return obj.getPattern();
            }
        }).setHeader("Pattern");
        
        treeGrid.addItemClickListener(new TreeGridItemClickListener());
        treeGrid.addSelectionListener(new TreeGridSelectionListener());

        add(treeGrid);
        expand(treeGrid);

        HorizontalLayout hlayout = new HorizontalLayout();
        add(hlayout);

        addAll(model);

        setButtonsEnabled();

        grid.addColumn(Record::getName).setHeader("Name");
        grid.addColumn(Record::getDescription).setHeader("Description");
        grid.addColumn(Record::getType).setHeader("Type");

        expandAll(null);
    }

    protected List<ModelSchemaObject> getAllItems() {
        List<ModelSchemaObject> itemList = new ArrayList<ModelSchemaObject>();
        addItemsRecursively(null, itemList);
        return itemList;
    }
    
    protected void addItemsRecursively(ModelSchemaObject item, List<ModelSchemaObject> list) {
        if (item != null) {
            list.add(item);
        }
        for (ModelSchemaObject child : treeGrid.getTreeData().getChildren(item)) {
            addItemsRecursively(child, list);
        }
    }

    protected void collapseAll() {
        for (ModelSchemaObject itemId : getAllItems()) {
            treeGrid.collapse(itemId);
        }
    }

    protected void expandAll(ModelSchemaObject parentObject) {

        ModelSchemaObject schemaObject=null;
        if (parentObject == null) {
            for (Object itemId : getAllItems()) {
                schemaObject = (ModelSchemaObject) itemId;
            }
        } else {
            schemaObject = parentObject;
        }
        if (schemaObject != null) {
            treeGrid.expand(schemaObject);
            for (ModelSchemaObject childObject : schemaObject.getChildObjects()) {
                expandAll(childObject);
            }
        }
    }

    public void setButtonsEnabled() {
        if (!readOnly) {
            Set<ModelSchemaObject> selected = getSelectedItems();
            removeButton.setEnabled(selected.size() > 0);
            editButton.setEnabled(selected.size() > 0);
        } else {
            addSchemaObjectButton.setEnabled(false);
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
        }
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
        enterKeyRegistration = UI.getCurrent().addShortcutListener(() -> {
            lastEditItemIds = Collections.emptySet();
            treeGrid.getDataProvider().refreshAll();
        }, Key.ENTER);
    }

    @Override
    public void deselected() {
        if (enterKeyRegistration != null) {
            enterKeyRegistration.remove();
        }
    }

    protected Object getSelected() {
        if (getSelectedItems().size() > 0) {
            return getSelectedItems().iterator().next();
        } else {
            return null;
        }
    }

    protected Set<ModelSchemaObject> getSelectedItems() {
        return treeGrid.getSelectedItems();
    }

    protected void selectOnly(ModelSchemaObject itemId) {
        treeGrid.deselectAll();
        treeGrid.select(itemId);
    }

    protected void addAll(HierarchicalModel model) {
		ModelSchemaObject rootObject = model.getRootObject();
		if (rootObject != null) {
			addSchemaObject(rootObject, null, null);
		}
		treeGrid.getDataProvider().refreshAll();
    }

    protected void addSchemaObject(ModelSchemaObject schemaObject, ModelSchemaObject parent, ModelSchemaObject refObject) {
        treeGrid.getTreeData().addItem(parent, schemaObject);
        for (ModelSchemaObject childObject : schemaObject.getChildObjects()) {
            addSchemaObject(childObject, schemaObject, null);
        }
    }    
    
    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        treeGrid.getDataProvider().refreshAll();
        grid.getDataProvider().refreshAll();
    }

    class AddSchemaObjectClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
        		
            Set<ModelSchemaObject> selectedIds = getSelectedItems();
            if (selectedIds.size() > 1) {
            		//TODO: pop up a message and throw an error
            		// 		that 0 or 1 items must be selected to add an entity
            } else if (selectedIds.size() == 0 && model.getRootObject() != null && 
                model.getRootObject().getChildObjects().size() != 0) {
            		//TODO: pop up a message and throw an error
            		//		that if an entity already exists (root entity), the subsequent entities must be attached somewhere            		
            } else {
                ModelSchemaObject parentSchemaObject=null;
            		if (selectedIds.size()!=0) {
            			Object itemId = selectedIds.iterator().next();
            			parentSchemaObject = (ModelSchemaObject) itemId;
            		}
            		//add the schema object
            		ModelSchemaObject newObject = new ModelSchemaObject();
            		newObject.setModelId(model.getId());
            		newObject.setName("New Schema Object");
            		newObject.setId(UUID.randomUUID().toString());
            		if (parentSchemaObject != null) {
            		    newObject.setParentId(parentSchemaObject.getId());
            		}
            		context.getConfigurationService().save(newObject);
            		if (model.getRootObject() == null) {
            		    model.setRootObject(newObject);
            		} else {
            		    parentSchemaObject.getChildObjects().add(newObject);
            		}
                addSchemaObject(newObject,parentSchemaObject,null);
                treeGrid.expand(parentSchemaObject);
                selectOnly(newObject);
                editSelectedItem();
            }
        }
    }

    class EditClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            editSelectedItem();
        }
    }

    class RemoveClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {

            Set<ModelSchemaObject> selectedItems = getSelectedItems();
            ConfirmDialog.show("Delete?",
                    "Are you sure you want to delete the " + selectedItems.size() + " selected items and their children?",
                    ()->{
                        for (Object itemToDelete : selectedItems) {
                    		    context.getConfigurationService().delete((ModelSchemaObject) itemToDelete);
                    		    context.getConfigurationService().refresh(model);
                    		    treeGrid.getTreeData().clear();
                    		    addAll(model);
                        }
                    		return true;                    	
                    });
        }
    }

    class TreeGridItemClickListener implements ComponentEventListener<ItemClickEvent<ModelSchemaObject>> {
        long lastClick;

        public void onComponentEvent(ItemClickEvent<ModelSchemaObject> event) {
            if (event.getClickCount() == 2) {
                editSelectedItem();
            } else if (System.currentTimeMillis() - lastClick > 1000
                    && getSelectedItems().size() > 0) {
                treeGrid.deselectAll();
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TreeGridSelectionListener implements SelectionListener<Grid<ModelSchemaObject>, ModelSchemaObject> {
        public void selectionChange(SelectionEvent<Grid<ModelSchemaObject>, ModelSchemaObject> event) {
            lastEditItemIds = Collections.emptySet();
            treeGrid.getDataProvider().refreshAll();
            setButtonsEnabled();
        }
    }

    public class Record {
        
        ModelSchemaObject modelSchemaObject;

        String name = "";

        String description = "";
        
        String type = "";       
        
        String pattern = "";
        
        boolean required = false;

        public Record(ModelSchemaObject modelSchemaObject) {
            this.modelSchemaObject = modelSchemaObject;
            
            if (modelSchemaObject != null) {
                this.name = modelSchemaObject.getName();
                this.description = modelSchemaObject.getDescription();
                this.type = modelSchemaObject.getType();
                this.pattern = modelSchemaObject.getPattern();
                this.required = modelSchemaObject.isRequired();
            }
        }

        public int hashCode() {
            return modelSchemaObject.hashCode();
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        } 
    }
}
