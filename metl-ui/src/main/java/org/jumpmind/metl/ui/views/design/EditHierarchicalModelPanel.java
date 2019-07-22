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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.HierarchicalDataType;
import org.jumpmind.metl.core.model.HierarchicalModel;
import org.jumpmind.metl.core.model.ModelSchemaObject;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.v7.event.ItemClickEvent;
import com.vaadin.v7.event.ItemClickEvent.ItemClickListener;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.Table.ColumnGenerator;
import com.vaadin.v7.ui.TreeTable;
import com.vaadin.v7.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditHierarchicalModelPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TreeTable treeTable = new TreeTable();

    Table table = new Table();

    HierarchicalModel model;

    Set<Object> lastEditItemIds = Collections.emptySet();

    TableColumnSelectWindow tableColumnSelectWindow;

    Button addSchemaObjectButton;

    Button editButton;

    Button removeButton;

    ShortcutListener enterKeyListener;

    boolean readOnly;

    BeanItemContainer<Record> container = new BeanItemContainer<Record>(Record.class);

    public EditHierarchicalModelPanel(ApplicationContext context, String modelId, boolean readOnly) {
        this.context = context;
        this.model = new HierarchicalModel(modelId);
        this.readOnly = readOnly;
        context.getConfigurationService().refresh(model);
        ButtonBar buttonBar1 = new ButtonBar();
        addComponent(buttonBar1);
        
        addSchemaObjectButton = buttonBar1.addButton("Add SchemaObject", FontAwesome.PLUS);
        addSchemaObjectButton.addClickListener(new AddSchemaObjectClickListener());
        
        editButton = buttonBar1.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());
        
        removeButton = buttonBar1.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());

        treeTable.setSizeFull();
        treeTable.setCacheRate(100);
        treeTable.setPageLength(100);
        treeTable.setImmediate(true);
        treeTable.setSelectable(true);
        treeTable.setMultiSelect(true);
        treeTable.addGeneratedColumn("name", new ColumnGenerator() {
            public Object generateCell(Table source, Object itemId, Object columnId) {
                final AbstractNamedObject obj = (AbstractNamedObject) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    ImmediateUpdateTextField t = new ImmediateUpdateTextField(null) {
                        protected void save(String text) {
                            String newName = trim(text);
                            obj.setName(newName);
                            EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);                            
                        };
                    };
                    t.setWidth(100, Unit.PERCENTAGE);
                    t.setValue(obj.getName());
                    t.focus();
                    t.selectAll();
                    return t;
                } else {
                    return obj.getName();
                }
            }
        });
        treeTable.setColumnHeader("name", "Name");

        treeTable.addGeneratedColumn("description", new ColumnGenerator() {
            public Object generateCell(Table source, Object itemId, Object columnId) {
                final ModelSchemaObject obj = (ModelSchemaObject) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    ImmediateUpdateTextField t = new ImmediateUpdateTextField(null) {
                        protected void save(String text) {
                            obj.setDescription(trim(text));
                            EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                        };
                    };
                    t.setWidth(100, Unit.PERCENTAGE);
                    t.setValue(obj.getDescription());
                    return t;
                } else {
                    return obj.getDescription();
                }
            }
        });
        treeTable.setColumnHeader("description", "Description");

        treeTable.addGeneratedColumn("type", new ColumnGenerator() {
            public Object generateCell(Table source, Object itemId, Object columnId) {

                final ModelSchemaObject obj = (ModelSchemaObject) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    final ComboBox cbox = new ComboBox();
                    cbox.setNullSelectionAllowed(false);
                    for (HierarchicalDataType dataType : HierarchicalDataType.values()) {
                        cbox.addItem(dataType.name());
                    }
                    cbox.setValue(obj.getType());
                    cbox.addValueChangeListener(new ValueChangeListener() {
                        public void valueChange(ValueChangeEvent event) {
                            obj.setType((String) cbox.getValue());
                            EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                        }
                    });
                    cbox.addBlurListener(new BlurListener() {
                        public void blur(BlurEvent event) {
                            Collection<?> items = treeTable.getItemIds();
                            boolean found = false;
                            for (Object item : items) {
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
            }
        });
        treeTable.setColumnHeader("type", "Type");

        treeTable.addGeneratedColumn("pattern", new ColumnGenerator() {
            public Object generateCell(Table source, Object itemId, Object columnId) {
                final ModelSchemaObject obj = (ModelSchemaObject) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    ImmediateUpdateTextField t = new ImmediateUpdateTextField(null) {
                        protected void save(String text) {
                            obj.setPattern(trim(text));
                            EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                        };
                    };
                    t.setWidth(100, Unit.PERCENTAGE);
                    t.setValue(obj.getPattern());
                    return t;
                } else {
                    return obj.getPattern();
                }
            }
        });
        treeTable.setColumnHeader("pattern", "Pattern");
        
        treeTable.addItemClickListener(new TreeTableItemClickListener());
        treeTable.addValueChangeListener(new TreeTableValueChangeListener());
        enterKeyListener = new ShortcutListener("Enter", KeyCode.ENTER, null) {
            public void handleAction(Object sender, Object target) {
                lastEditItemIds = Collections.emptySet();
                treeTable.refreshRowCache();
            }
        };

        addComponent(treeTable);
        setExpandRatio(treeTable, 1.0f);

        HorizontalLayout hlayout = new HorizontalLayout();
        addComponent(hlayout);

        addAll(model);

        setButtonsEnabled();

        table.setContainerDataSource(container);
        table.setVisibleColumns(
                new Object[] { "name","description", "type"});
        table.setColumnHeaders(
                new String[] { "name", "Description", "Type"});

        expandAll(null);
    }

    protected void collapseAll() {
        for (Object itemId : treeTable.getItemIds()) {
            treeTable.setCollapsed(itemId, true);
        }
    }

    protected void expandAll(ModelSchemaObject parentObject) {

        ModelSchemaObject schemaObject=null;
        if (parentObject == null) {
            for (Object itemId : treeTable.getItemIds()) {
                schemaObject = (ModelSchemaObject) itemId;
            }
        } else {
            schemaObject = parentObject;
        }
        if (schemaObject != null) {
            treeTable.setCollapsed(schemaObject, false);
            for (ModelSchemaObject childObject : schemaObject.getChildObjects()) {
                expandAll(childObject);
            }
        }
    }

    public void setButtonsEnabled() {
        if (!readOnly) {
            Set<Object> selected = getSelectedItems();
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
        treeTable.addShortcutListener(enterKeyListener);
    }

    @Override
    public void deselected() {
        treeTable.removeShortcutListener(enterKeyListener);
    }

    protected Object getSelected() {
        if (getSelectedItems().size() > 0) {
            return getSelectedItems().iterator().next();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected Set<Object> getSelectedItems() {
        return (Set<Object>) treeTable.getValue();
    }

    protected void selectOnly(Object itemId) {
        for (Object id : getSelectedItems()) {
            treeTable.unselect(id);
        }
        treeTable.select(itemId);
    }

    protected void addAll(HierarchicalModel model) {
		ModelSchemaObject rootObject = model.getRootObject();
		if (rootObject != null) {
			addSchemaObject(rootObject, null, null);
		}
    }

    protected void addSchemaObject(ModelSchemaObject schemaObject, ModelSchemaObject parent, ModelSchemaObject refObject) {
        treeTable.addItem(schemaObject);
        treeTable.setChildrenAllowed(schemaObject, true);
        treeTable.setItemIcon(schemaObject, FontAwesome.CUBE);
        if (parent != null) {
            treeTable.setParent(schemaObject, parent);
        }
        for (ModelSchemaObject childObject : schemaObject.getChildObjects()) {
            addSchemaObject(childObject, schemaObject, null);
        }
    }    
    
    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        treeTable.refreshRowCache();
        table.refreshRowCache();
    }

    class AddSchemaObjectClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        		
            Set<Object> selectedIds = getSelectedItems();
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
                treeTable.setCollapsed(parentSchemaObject, false);
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

            Set<Object> selectedItems = getSelectedItems();
            ConfirmDialog.show("Delete?",
                    "Are you sure you want to delete the " + selectedItems.size() + " selected items and their children?",
                    ()->{
                        for (Object itemToDelete : selectedItems) {
                    		    context.getConfigurationService().delete((ModelSchemaObject) itemToDelete);
                    		    context.getConfigurationService().refresh(model);
                    		    treeTable.removeAllItems();
                    		    addAll(model);
                        }
                    		return true;                    	
                    });
        }
    }

    class TreeTableItemClickListener implements ItemClickListener {
        long lastClick;

        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                editSelectedItem();
            } else if (System.currentTimeMillis() - lastClick > 1000
                    && getSelectedItems().size() > 0) {
                treeTable.setValue(null);
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TreeTableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            lastEditItemIds = Collections.emptySet();
            treeTable.refreshRowCache();
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
