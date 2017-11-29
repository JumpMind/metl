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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditRelationalModelPanel extends VerticalLayout implements IUiPanel {

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

    Button moveUpButton;

    Button moveDownButton;

    Button moveTopButton;

    Button moveBottomButton;

    TextField filterField;

    ShortcutListener enterKeyListener;

    boolean readOnly;

    BeanItemContainer<Record> container = new BeanItemContainer<Record>(Record.class);

    public EditRelationalModelPanel(ApplicationContext context, String modelId, boolean readOnly) {
        this.context = context;
        this.model = new Model(modelId);
        this.readOnly = readOnly;
        context.getConfigurationService().refresh(model);

        ButtonBar buttonBar1 = new ButtonBar();
        addComponent(buttonBar1);
        ButtonBar buttonBar2 = new ButtonBar();
        addComponent(buttonBar2);

            addEntityButton = buttonBar1.addButton("Add Entity", FontAwesome.TABLE);
            addEntityButton.addClickListener(new AddEntityClickListener());

            addAttributeButton = buttonBar1.addButton("Add Attr", FontAwesome.COLUMNS);
            addAttributeButton.addClickListener(new AddAttributeClickListener());

            editButton = buttonBar1.addButton("Edit", FontAwesome.EDIT);
            editButton.addClickListener(new EditClickListener());

            removeButton = buttonBar1.addButton("Remove", FontAwesome.TRASH_O);
            removeButton.addClickListener(new RemoveClickListener());

            moveUpButton = buttonBar2.addButton("Up", FontAwesome.ARROW_UP, e -> moveUp());
            moveDownButton = buttonBar2.addButton("Down", FontAwesome.ARROW_DOWN, e -> moveDown());
            moveTopButton = buttonBar2.addButton("Top", FontAwesome.ANGLE_DOUBLE_UP,
                    e -> moveTop());
            moveBottomButton = buttonBar2.addButton("Bottom", FontAwesome.ANGLE_DOUBLE_DOWN,
                    e -> moveBottom());

            importButton = buttonBar1.addButtonRight("Import ...", FontAwesome.UPLOAD,
                    new ImportClickListener());

        buttonBar1.addButtonRight("Export...", FontAwesome.DOWNLOAD, (e) -> export());

        filterField = buttonBar2.addFilter();
        filterField.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                filterField.setValue(event.getText());
                treeTable.removeAllItems();
                addAll(event.getText(), EditRelationalModelPanel.this.model.getModelEntities());
            }
        });

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
                                EditRelationalModelPanel.this.context.getConfigurationService().save(obj);
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
                    return UiUtils.getName(filterField.getValue(), obj.getName());
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
                                EditRelationalModelPanel.this.context.getConfigurationService().save(obj);
                            };
                        };
                        t.setWidth(100, Unit.PERCENTAGE);
                        t.setValue(obj.getDescription());
                        return t;
                    } else {
                        return UiUtils.getName(filterField.getValue(), obj.getDescription());
                    }
                }
                if (itemId instanceof ModelEntity) {
                    final ModelEntity obj = (ModelEntity) itemId;
                    if (lastEditItemIds.contains(itemId) && !readOnly) {
                        ImmediateUpdateTextField t = new ImmediateUpdateTextField(null) {
                            protected void save(String text) {                                
                                obj.setDescription(trim(text));
                                EditRelationalModelPanel.this.context.getConfigurationService().save(obj);
                            };
                        };
                        t.setWidth(100, Unit.PERCENTAGE);
                        t.setValue(obj.getDescription());
                        return t;
                    } else {
                        return UiUtils.getName(filterField.getValue(), obj.getDescription());
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
                        for (DataType dataType : DataType.values()) {
                            cbox.addItem(dataType.name());
                        }
                        cbox.setValue(obj.getType());
                        cbox.addValueChangeListener(new ValueChangeListener() {
                            public void valueChange(ValueChangeEvent event) {
                                obj.setType((String) cbox.getValue());
                                EditRelationalModelPanel.this.context.getConfigurationService().save(obj);
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

        treeTable.addGeneratedColumn("pk", new ColumnGenerator() {
            public Object generateCell(Table source, Object itemId, Object columnId) {
                if (itemId instanceof ModelAttrib) {
                    final ModelAttrib obj = (ModelAttrib) itemId;
                    if (lastEditItemIds.contains(itemId) && !readOnly) {
                        final CheckBox cbox = new CheckBox();
                        cbox.setValue(obj.isPk());
                        cbox.setImmediate(true);
                        cbox.addValueChangeListener(event -> togglePk(obj));
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
                    } else if (obj.isPk()) {
                        return new Label(FontAwesome.KEY.getHtml(), ContentMode.HTML);
                    }
                }
                return null;
            }
        });
        treeTable.setColumnHeader("pk", "PK");
        treeTable.setColumnWidth("pk", 40);

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

        addAll("", model.getModelEntities());

        setButtonsEnabled();

        table.setContainerDataSource(container);
        table.setVisibleColumns(
                new Object[] { "entityName", "attributeName", "description", "type", "pk" });
        table.setColumnHeaders(
                new String[] { "Entity Name", "Attribute Name", "Description", "Type", "PK" });

        if (model.getModelEntities().size() > 10) {
            collapseAll();
        }
    }

    protected void move(boolean down, boolean toEnd) {
        ModelAttrib selected = (ModelAttrib) getSelected();
        if (selected != null) {
            ModelEntity entity = (ModelEntity) treeTable.getParent(selected);
            List<ModelAttrib> attributes = entity.getModelAttributes();
            int index = attributes.indexOf(selected);
            if (down && index < attributes.size() - 1 && !toEnd) {
                attributes.remove(selected);
                attributes.add(index + 1, selected);
            } else if (!down && index > 0 && !toEnd) {
                attributes.remove(selected);
                attributes.add(index - 1, selected);

            } else if (down) {
                attributes.remove(selected);
                attributes.add(0, selected);
            } else {
                attributes.remove(selected);
                attributes.add(selected);
            }

            index = 0;
            for (ModelAttrib modelAttribute : attributes) {
                modelAttribute.setAttributeOrder(index++);
                context.getConfigurationService().save(modelAttribute);
            }

            Collection<?> children = new ArrayList<>(treeTable.getChildren(entity));
            for (Object object : children) {
                treeTable.removeItem(object);
            }

            for (ModelAttrib modelAttribute : attributes) {
                addModelAttribute(entity, modelAttribute);
            }
            treeTable.select(selected);

        }
    }

    protected void moveDown() {
        move(true, false);
    }

    protected void moveUp() {
        move(false, false);
    }

    protected void moveTop() {
        move(true, true);
    }

    protected void moveBottom() {
        move(false, true);
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
        updateExportTable(filterField.getValue(), model.getModelEntities());
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

            boolean enableMove = selected.size() == 1
                    && selected.iterator().next() instanceof ModelAttrib;
            moveBottomButton.setEnabled(enableMove);
            moveTopButton.setEnabled(enableMove);
            moveUpButton.setEnabled(enableMove);
            moveDownButton.setEnabled(enableMove);
        } else {
            addEntityButton.setEnabled(false);
            addAttributeButton.setEnabled(false);
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
            moveBottomButton.setEnabled(false);
            moveTopButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
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

    protected void add(ModelEntity modelEntity) {
        addModelEntity(modelEntity);
        for (ModelAttrib modelAttribute : modelEntity.getModelAttributes()) {
            treeTable.setChildrenAllowed(modelEntity, true);
            modelAttribute.setEntityId(modelEntity.getId());
            addModelAttribute(modelEntity, modelAttribute);
        }
        treeTable.setCollapsed(modelEntity, false);
    }

    protected void addAll(String filter, Collection<ModelEntity> modelEntityList) {
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
            add(modelEntity);
        }
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

    protected void addModelEntity(ModelEntity modelEntity) {
        treeTable.addItem(modelEntity);
        treeTable.setItemIcon(modelEntity, FontAwesome.TABLE);
        treeTable.setChildrenAllowed(modelEntity, false);
    }

    protected void addModelAttribute(ModelEntity entity, ModelAttrib modelAttribute) {
        treeTable.addItem(modelAttribute);
        treeTable.setItemIcon(modelAttribute, FontAwesome.COLUMNS);
        treeTable.setChildrenAllowed(entity, true);
        treeTable.setParent(modelAttribute, entity);
        treeTable.setChildrenAllowed(modelAttribute, false);
    }

    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        treeTable.refreshRowCache();
        table.refreshRowCache();
    }

    class AddEntityClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            ModelEntity e = new ModelEntity();
            e.setName("New Entity");
            e.setModelId(model.getId());
            model.getModelEntities().add(e);
            context.getConfigurationService().save(e);
            addModelEntity(e);
            selectOnly(e);
            editSelectedItem();
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
                if (itemId instanceof ModelEntity) {
                    entity = (ModelEntity) itemId;
                } else if (itemId instanceof ModelAttrib) {
                    entity = (ModelEntity) treeTable.getParent(itemId);
                }

                if (entity != null) {
                    a.setEntityId(entity.getId());
                    entity.addModelAttribute(a);
                    context.getConfigurationService().save(a);
                    addModelAttribute(entity, a);
                    treeTable.setCollapsed(entity, false);
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
        @SuppressWarnings("unchecked")
        public void buttonClick(ClickEvent event) {

            Set<Object> itemIds = new HashSet<Object>();
            Set<Object> selectedIds = getSelectedItems();
            
            ConfirmDialog.show("Delete?",
                    "Are you sure you want to delete the " + selectedIds.size() + " selected items?",
                    ()->{
                        for (Object itemId : selectedIds) {
                            Collection<Object> children = (Collection<Object>) treeTable
                                    .getContainerDataSource().getChildren(itemId);
                            if (children != null) {
                                itemIds.addAll(children);
                            }
                            itemIds.add(itemId);
                        }

                        for (Object itemId : itemIds) {
                            if (itemId instanceof ModelAttrib) {
                                ModelAttrib a = (ModelAttrib) itemId;
                                context.getConfigurationService().delete((ModelAttrib) itemId);
                                ModelEntity entity = (ModelEntity) treeTable.getParent(itemId);
                                entity.removeModelAttribute(a);
                                treeTable.removeItem(itemId);
                            }
                        }
                        for (Object itemId : itemIds) {
                            if (itemId instanceof ModelEntity) {
                                context.getConfigurationService().delete((ModelEntity) itemId);
                                treeTable.removeItem(itemId);
                                model.getModelEntities().remove(itemId);
                            }
                        }

                        return true;
                    });
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
                    add(e);
                    model.getModelEntities().add(e);
                } else {
                    for (ModelAttrib a : e.getModelAttributes()) {
                        if (modelEntity.getModelAttributeByName(a.getName()) == null) {
                            a.setEntityId(modelEntity.getId());
                            context.getConfigurationService().save(a);
                            modelEntity.addModelAttribute(a);
                            addModelAttribute(modelEntity, a);
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

        String pk = "";

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
                this.pk = modelAttribute.isPk() ? "PK" : "";
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

        public String getPk() {
            return pk;
        }

        public void setPk(String pk) {
            this.pk = pk;
        }
    }
}
