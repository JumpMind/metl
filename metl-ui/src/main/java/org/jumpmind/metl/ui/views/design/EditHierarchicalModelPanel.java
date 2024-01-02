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

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.ItemClickListener;

@SuppressWarnings("serial")
public class EditHierarchicalModelPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TreeGrid<ModelSchemaObject> treeGrid = new TreeGrid<ModelSchemaObject>();

    Grid<Record> grid = new Grid<Record>();

    HierarchicalModel model;

    Set<ModelSchemaObject> lastEditItemIds = Collections.emptySet();

    TableColumnSelectWindow tableColumnSelectWindow;

    Button addSchemaObjectButton;

    Button editButton;

    Button removeButton;

    ShortcutListener enterKeyListener;
    
    Registration enterKeyRegistration;

    boolean readOnly;

    public EditHierarchicalModelPanel(ApplicationContext context, String modelId, boolean readOnly) {
        this.context = context;
        this.model = new HierarchicalModel(modelId);
        this.readOnly = readOnly;
        context.getConfigurationService().refresh(model);
        ButtonBar buttonBar1 = new ButtonBar();
        addComponent(buttonBar1);
        
        addSchemaObjectButton = buttonBar1.addButton("Add SchemaObject", VaadinIcons.PLUS);
        addSchemaObjectButton.addClickListener(new AddSchemaObjectClickListener());
        
        editButton = buttonBar1.addButton("Edit", VaadinIcons.EDIT);
        editButton.addClickListener(new EditClickListener());
        
        removeButton = buttonBar1.addButton("Remove", VaadinIcons.TRASH);
        removeButton.addClickListener(new RemoveClickListener());

        treeGrid.setSizeFull();
        //treeGrid.setCacheRate(100);
        //treeGrid.setPageLength(100);
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
                t.setWidth(100, Unit.PERCENTAGE);
                t.setValue(obj.getName());
                t.focus();
                t.selectAll();
                t.setIcon(VaadinIcons.CUBE);
                return t;
            } else {
                Label label = new Label(obj.getName());
                label.setIcon(VaadinIcons.CUBE);
                return label;
            }
        }).setCaption("Name");

        treeGrid.addColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                TextField t = new TextField();
                t.setValueChangeMode(ValueChangeMode.LAZY);
                t.setValueChangeTimeout(200);
                t.addValueChangeListener(event -> {
                    obj.setDescription(trim(event.getValue()));
                    EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                });
                t.setWidth(100, Unit.PERCENTAGE);
                t.setValue(obj.getDescription());
                return t;
            } else {
                return obj.getDescription();
            }
        }).setCaption("Description");

        treeGrid.addColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                final ComboBox<String> cbox = new ComboBox<String>();
                cbox.setEmptySelectionAllowed(false);
                List<String> dataTypeList = new ArrayList<String>();
                for (HierarchicalDataType dataType : HierarchicalDataType.values()) {
                    dataTypeList.add(dataType.name());
                }
                cbox.setItems(dataTypeList);
                cbox.setValue(obj.getType());
                cbox.addValueChangeListener(new ValueChangeListener<String>() {
                    public void valueChange(ValueChangeEvent<String> event) {
                        obj.setType(cbox.getValue());
                        EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                    }
                });
                cbox.addBlurListener(new BlurListener() {
                    public void blur(BlurEvent event) {
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
        }).setCaption("Type");

        treeGrid.addColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                TextField t = new TextField();
                t.setValueChangeMode(ValueChangeMode.LAZY);
                t.setValueChangeTimeout(200);
                t.addValueChangeListener(event -> {
                    obj.setPattern(trim(event.getValue()));
                    EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                });
                t.setWidth(100, Unit.PERCENTAGE);
                t.setValue(obj.getPattern());
                return t;
            } else {
                return obj.getPattern();
            }
        }).setCaption("Pattern");
        
        treeGrid.addItemClickListener(new TreeGridItemClickListener());
        treeGrid.addSelectionListener(new TreeGridSelectionListener());
        enterKeyListener = new ShortcutListener("Enter", KeyCode.ENTER, null) {
            public void handleAction(Object sender, Object target) {
                lastEditItemIds = Collections.emptySet();
                treeGrid.getDataProvider().refreshAll();
            }
        };

        addComponent(treeGrid);
        setExpandRatio(treeGrid, 1.0f);

        HorizontalLayout hlayout = new HorizontalLayout();
        addComponent(hlayout);

        addAll(model);

        setButtonsEnabled();

        grid.addColumn(Record::getName).setCaption("Name");
        grid.addColumn(Record::getDescription).setCaption("Description");
        grid.addColumn(Record::getType).setCaption("Type");

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
        enterKeyRegistration = treeGrid.addShortcutListener(enterKeyListener);
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

    class AddSchemaObjectClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        		
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

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            editSelectedItem();
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {

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

    class TreeGridItemClickListener implements ItemClickListener<ModelSchemaObject> {
        long lastClick;

        public void itemClick(ItemClick<ModelSchemaObject> event) {
            if (event.getMouseEventDetails().isDoubleClick()) {
                editSelectedItem();
            } else if (System.currentTimeMillis() - lastClick > 1000
                    && getSelectedItems().size() > 0) {
                treeGrid.deselectAll();
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TreeGridSelectionListener implements SelectionListener<ModelSchemaObject> {
        public void selectionChange(SelectionEvent<ModelSchemaObject> event) {
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
