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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelEntitySorter;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.ExportDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.jumpmind.vaadin.ui.common.NotifyDialog;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditHierarchicalModelPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TreeTable treeTable = new TreeTable();

    Table table = new Table();

    Model model;

    Set<Object> lastEditItemIds = Collections.emptySet();

    TableColumnSelectWindow tableColumnSelectWindow;

    Button addEntityButton;

    Button addAttributeButton;

    Button editButton;

    Button removeButton;

    Button importButton;

    ShortcutListener enterKeyListener;

    boolean readOnly;

    BeanItemContainer<Record> container = new BeanItemContainer<Record>(Record.class);

    public EditHierarchicalModelPanel(ApplicationContext context, String modelId, boolean readOnly) {
        this.context = context;
        this.model = new Model(modelId);
        this.readOnly = readOnly;
        context.getConfigurationService().refresh(model);

        ButtonBar buttonBar1 = new ButtonBar();
        addComponent(buttonBar1);

        addEntityButton = buttonBar1.addButton("Add Entity", FontAwesome.TABLE);
        addEntityButton.addClickListener(new AddEntityClickListener());

        addAttributeButton = buttonBar1.addButton("Add Attr", FontAwesome.COLUMNS);
        addAttributeButton.addClickListener(new AddAttributeClickListener());

        editButton = buttonBar1.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar1.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());

        importButton = buttonBar1.addButtonRight("Import ...", FontAwesome.UPLOAD,
                new ImportClickListener());

        buttonBar1.addButtonRight("Export...", FontAwesome.DOWNLOAD, (e) -> export());

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
                            boolean unique = true;
                            if (obj instanceof ModelEntity) {
                                List<ModelEntity> entities = model.getModelEntities();
                                for (ModelEntity entity : entities) {
                                    if (!entity.equals(obj) && entity.getName().equals(newName)) {
                                        unique = false;
                                    }
                                }
                            } else if (obj instanceof ModelAttrib) {
                                List<ModelAttrib> attributes = model.getEntityById(((ModelAttrib)obj).getEntityId()).getModelAttributes();
                                for (ModelAttrib attribute : attributes) {
                                    if (!attribute.equals(obj) && attribute.getName().equals(newName)) {
                                        unique = false;
                                    }
                                }
                            }
                            if (unique) {
                                obj.setName(newName);
                                EditHierarchicalModelPanel.this.context.getConfigurationService().save(obj);
                            } else {
                                NotifyDialog.show("Name needs to be unique", "Name needs to be unique", null, Type.WARNING_MESSAGE);
                            }
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
                if (itemId instanceof ModelAttrib) {
                    final ModelAttrib obj = (ModelAttrib) itemId;
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
                if (itemId instanceof ModelEntity) {
                    final ModelEntity obj = (ModelEntity) itemId;
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
                } else
                    return null;
            }
        });
        treeTable.setColumnHeader("description", "Description");

        treeTable.addGeneratedColumn("type", new ColumnGenerator() {
            public Object generateCell(Table source, Object itemId, Object columnId) {
                if (itemId instanceof ModelAttrib) {
                    final ModelAttrib obj = (ModelAttrib) itemId;
                    if (lastEditItemIds.contains(itemId) && !readOnly) {
                        final ComboBox cbox = new ComboBox();
                        cbox.setNullSelectionAllowed(false);
                        if (obj.getTypeEntityId() == null) {
                            for (DataType dataType : DataType.values()) {
                                cbox.addItem(dataType.name());
                            }
                        } else {
                            cbox.addItem(DataType.ARRAY.name());
                            cbox.addItem(DataType.REF.name());                            
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
                } else {
                    return null;
                }
            }
        });
        treeTable.setColumnHeader("type", "Type");

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

        Button collapseAll = new Button("Collapse All");
        collapseAll.addStyleName(ValoTheme.BUTTON_LINK);
        collapseAll.addStyleName(ValoTheme.BUTTON_SMALL);
        hlayout.addComponent(collapseAll);
        collapseAll.addClickListener(e -> collapseAll());

        Button expandAll = new Button("Expand All");
        expandAll.addStyleName(ValoTheme.BUTTON_LINK);
        expandAll.addStyleName(ValoTheme.BUTTON_SMALL);
        hlayout.addComponent(expandAll);
        expandAll.addClickListener(e -> expandAll());

        addAll(model);

        setButtonsEnabled();

        table.setContainerDataSource(container);
        table.setVisibleColumns(
                new Object[] { "entityName", "attributeName", "description", "type"});
        table.setColumnHeaders(
                new String[] { "Entity Name", "Attribute Name", "Description", "Type"});

        if (model.getModelEntities().size() > 10) {
            collapseAll();
        }
    }

    protected void collapseAll() {
        for (Object itemId : treeTable.getItemIds()) {
            treeTable.setCollapsed(itemId, true);
        }
    }

    protected void expandAll() {
        for (Object itemId : treeTable.getItemIds()) {
            treeTable.setCollapsed(itemId, false);
        }
    }

    protected void export() {
        table.removeAllItems();
        updateExportTable(null, model.getModelEntities());
        String fileNamePrefix = model.getName().toLowerCase().replace(' ', '-');
        ExportDialog dialog = new ExportDialog(table, fileNamePrefix, model.getName());
        UI.getCurrent().addWindow(dialog);
    }

    protected void togglePk(ModelAttrib a) {
        a.setPk(!a.isPk());
        context.getConfigurationService().save(a);
    }

    public void setButtonsEnabled() {
        if (!readOnly) {
            Set<Object> selected = getSelectedItems();
            addAttributeButton.setEnabled(selected.size() > 0);
            removeButton.setEnabled(selected.size() > 0);
            editButton.setEnabled(selected.size() > 0);
        } else {
            addEntityButton.setEnabled(false);
            addAttributeButton.setEnabled(false);
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
            importButton.setEnabled(false);
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

    protected void addAll(Model model) {
		ModelEntity rootEntity = model.getRootElement();
		if (rootEntity != null) {
			addEntity(rootEntity, null, null);
		}
    }

    protected void addEntity(ModelEntity entity, ModelEntity parentEntity, ModelAttrib refAttribute) {
        if (parentEntity == null) {
            treeTable.addItem(entity);
            treeTable.setChildrenAllowed(entity, true);
            treeTable.setItemIcon(entity, FontAwesome.TABLE);                       
        }                
        for (ModelAttrib attrib:entity.getModelAttributes()) {
            if (refAttribute != null) {
                addAttribute(refAttribute, attrib); 
            } else {
                addAttribute(entity,attrib);
            }
        		if (attrib.getTypeEntityId() != null) {
        			if (attrib.getDataType().equals(DataType.ARRAY)) {
        				addEntity(model.getEntityById(attrib.getTypeEntityId()), entity, attrib);
        			} else {
        				addEntity(model.getEntityById(attrib.getTypeEntityId()), entity, attrib);        				
        			}
        		}
        }
    }

    protected void addAttribute(Object parent, ModelAttrib attribute) {
        treeTable.addItem(attribute);
        if (attribute.getDataType().equals(DataType.ARRAY)) {
            treeTable.setItemIcon(attribute, FontAwesome.LIST);
            treeTable.setChildrenAllowed(attribute, true);
        } else if (attribute.getDataType().equals(DataType.REF)) {
            treeTable.setItemIcon(attribute, FontAwesome.TABLE);
            treeTable.setChildrenAllowed(attribute, true);
        } else {
            treeTable.setItemIcon(attribute, FontAwesome.COLUMNS);
            treeTable.setChildrenAllowed(attribute, false);
        }
        treeTable.setParent(attribute, parent);
    }
        
    protected void updateExportTable(String filter, Collection<ModelEntity> modelEntityList) {
        filter = filter != null ? filter.toLowerCase() : null;
        ArrayList<ModelEntity> filteredModelEntityList = new ArrayList<ModelEntity>();
        for (ModelEntity modelEntity : modelEntityList) {
            boolean add = UiUtils.filterMatches(filter, modelEntity.getName());
            if (!add) {
                for (ModelAttrib modelAttribute : modelEntity.getModelAttributes()) {
                    add |= UiUtils.filterMatches(filter, modelAttribute.getName());
                }
            }
            if (add) {
                filteredModelEntityList.add(modelEntity);
            }
        }

        Collections.sort(filteredModelEntityList, new ModelEntitySorter());
        for (ModelEntity modelEntity : filteredModelEntityList) {
            for (ModelAttrib modelAttribute : modelEntity.getModelAttributes()) {
                table.addItem(new Record(modelEntity, modelAttribute));
            }
        }
    }

    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        treeTable.refreshRowCache();
        table.refreshRowCache();
    }

    class AddEntityClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        		
            Set<Object> selectedIds = getSelectedItems();
            if (selectedIds.size() > 1) {
            		//TODO: pop up a message and throw an error
            		// 		that 0 or 1 items must be selected to add an entity
            } else if (selectedIds.size() == 0 && model.getModelEntities().size() != 0) {
            		//TODO: pop up a message and throw an error
            		//		that if an entity already exists (root entity), the subsequent entities must be attached somewhere            		
            } else {
            		//need to create a model attribute that references a new entity as well as a new entity
            		ModelEntity parentEntity=null;
            		if (selectedIds.size()!=0) {
            			Object itemId = selectedIds.iterator().next();
                    if (itemId instanceof ModelEntity) {
                        parentEntity = (ModelEntity) itemId;
                    } else {
                        ModelAttrib selectedAttrib = (ModelAttrib) itemId;
                        if (selectedAttrib.getTypeEntityId() != null) {
                            parentEntity = model.getEntityById(selectedAttrib.getTypeEntityId());
                        } else {
                            Object parent = treeTable.getParent(itemId);
                            if (parent instanceof ModelEntity) {
                                parentEntity = (ModelEntity) parent;
                            } else {
                                ModelAttrib parentAttrib = (ModelAttrib) parent;
                                parentEntity = model.getEntityById(parentAttrib.getTypeEntityId());
                            }
                        }
                    }        			
            		}            		
            		//add the entity
            		ModelEntity childEntity = new ModelEntity();
            		childEntity.setModelId(model.getId());
            		childEntity.setName("New Entity");
            		childEntity.setId(UUID.randomUUID().toString());
            		model.getModelEntities().add(childEntity);
            		context.getConfigurationService().save(childEntity);
            		//add the attribute that references the entity
            		ModelAttrib attrib=null;
            		if (parentEntity != null) {
	            		attrib = new ModelAttrib();
	            		attrib.setEntityId(parentEntity.getId());
	            		attrib.setDataType(DataType.REF);
	            		attrib.setTypeEntityId(childEntity.getId());
	            		attrib.setName("entity ref");
	            		parentEntity.getModelAttributes().add(attrib);
	            		context.getConfigurationService().save(attrib);
                    treeTable.setCollapsed(parentEntity, false);
                    if (model.getRootElement().getId().equalsIgnoreCase(parentEntity.getId())) {
                        addAttribute(parentEntity, attrib);                        
                    } else {
                        addAttribute(model.getModelAttribByTypeEntityId(parentEntity.getId()),attrib);
                    }
            		} else {
            		    addEntity(childEntity, parentEntity, attrib);
            		}
            		if (attrib != null) {
            		    selectOnly(attrib);
            		} else {
            		    selectOnly(childEntity);
            		}
            		editSelectedItem();
            }
        }
    }

    class AddAttributeClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<Object> itemIds = getSelectedItems();
            if (itemIds.size() > 0) {
                ModelAttrib a = new ModelAttrib();
                a.setName("New Attribute");
                a.setDataType(DataType.VARCHAR);
                Object itemId = itemIds.iterator().next();
                ModelEntity entity = null;
                ModelAttrib selectedAttrib=null;
                if (itemId instanceof ModelAttrib) {
                    selectedAttrib = (ModelAttrib) itemId;
                    if (selectedAttrib.getTypeEntityId() != null) {
                        //an attribute referencing an entity
                        entity = model.getEntityById(selectedAttrib.getTypeEntityId());
                    } else {
                        //an attribute that has some parent
                        entity = model.getEntityById(selectedAttrib.getEntityId());
                        itemId = model.getModelAttribByTypeEntityId(entity.getId());
                        if (itemId == null) {
                            itemId = entity;
                        }
                    }
                } else {
                    entity = (ModelEntity) itemId;
                }
                if (entity != null) {
                    a.setEntityId(entity.getId());
                    entity.addModelAttribute(a);
                    context.getConfigurationService().save(a);
                    addAttribute(itemId, a);
                    treeTable.setCollapsed(itemId, false);
                    selectOnly(a);
                    editSelectedItem();
                }
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
                    "Are you sure you want to delete the " + selectedItems.size() + " selected items?",
                    ()->{
                    		deleteSelectedItems(selectedItems);
                    		return true;                    	
                    });
        }
        
        private void deleteSelectedItems(Set<Object> selectedItems) {
            for (Object itemToDelete : selectedItems) {
            		if (itemToDelete instanceof ModelAttrib) {
            			deleteAttribute((ModelAttrib)itemToDelete);
            		} else {
            			deleteEntity((ModelEntity)itemToDelete);
            		}
            }
        }

        private void deleteEntity(ModelEntity entity) {
        		if (model.getModelEntities().contains(entity)) {
        			List<ModelAttrib> attribs = new ArrayList<ModelAttrib>();
        			attribs.addAll(entity.getModelAttributes());
	        		for (ModelAttrib attrib:attribs) {
	        			if (attrib.getDataType().equals(DataType.ARRAY) || 
	        					attrib.getDataType().equals(DataType.REF)) {
	        				ModelEntity childEntity = model.getEntityById(attrib.getTypeEntityId());
	        				context.getConfigurationService().delete(attrib);
	        				entity.removeModelAttribute(attrib);
	        				deleteEntity(childEntity);
	        			} else {
	        				deleteAttribute(attrib);
	        			}
	        		}
	        		//if this entity has a parent, then we have to delete
	        		//both this entity as well as the attribute used to link
	        		//this entity
	        		ModelEntity parentEntity = (ModelEntity) treeTable.getParent(entity);
	        		if (parentEntity != null) {	        			
	        			ModelAttrib linkedAttrib = parentEntity.getModelAttributeByTypeEntityId(entity.getId());
	        			if (linkedAttrib != null) {
	        				deleteAttribute(linkedAttrib);
	        			}
	        		}
	        		context.getConfigurationService().delete(entity);
	            treeTable.removeItem(entity);
	            model.getModelEntities().remove(entity);
	        }
        }
    }

    private void deleteAttribute(ModelAttrib attribute) {
		//ModelEntity entity = (ModelEntity) treeTable.getParent(attribute);
		ModelEntity entity = model.getEntityById(attribute.getEntityId());
		if (entity.getModelAttributes().contains(attribute)) {
			context.getConfigurationService().delete(attribute);
			entity.removeModelAttribute(attribute);
			treeTable.removeItem(attribute);
		}
    }
    
    class ImportClickListener implements ClickListener, TableColumnSelectListener {
        public void buttonClick(ClickEvent event) {
            if (tableColumnSelectWindow == null) {
                tableColumnSelectWindow = new TableColumnSelectWindow(context, model);
                tableColumnSelectWindow.setTableColumnSelectListener(this);
            }
            UI.getCurrent().addWindow(tableColumnSelectWindow);
        }

        public void selected(Collection<ModelEntity> modelEntityCollection) {
            HashMap<String, ModelEntity> existingModelEntities = new HashMap<String, ModelEntity>();
            for (Object itemId : treeTable.getItemIds()) {
                if (itemId instanceof ModelEntity) {
                    ModelEntity modelEntity = (ModelEntity) itemId;
                    existingModelEntities.put(modelEntity.getName().toUpperCase(), modelEntity);
                }
            }

            for (ModelEntity e : modelEntityCollection) {
                ModelEntity modelEntity = existingModelEntities.get(e.getName().toUpperCase());
                if (modelEntity == null) {
                    context.getConfigurationService().save(e);
                    existingModelEntities.put(e.getName().toUpperCase(), e);
//TODO:                    add(e);
                    model.getModelEntities().add(e);
                } else {
                    for (ModelAttrib a : e.getModelAttributes()) {
                        if (modelEntity.getModelAttributeByName(a.getName()) == null) {
                            a.setEntityId(modelEntity.getId());
                            context.getConfigurationService().save(a);
                            modelEntity.addModelAttribute(a);
//TODO:                            addModelAttribute(modelEntity, a);
                        }
                    }
                }
            }
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
        ModelEntity modelEntity;

        ModelAttrib modelAttribute;

        String entityName = "";

        String attributeName = "";

        String description = "";

        String type = "";

        public Record(ModelEntity modelEntity, ModelAttrib modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;

            if (modelEntity != null) {
                this.entityName = modelEntity.getName();
            }

            if (modelAttribute != null) {
                this.attributeName = modelAttribute.getName();
                this.description = modelAttribute.getDescription();
                this.type = modelAttribute.getType();
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
    }
}
