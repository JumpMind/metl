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
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelEntitySorter;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.NotifyDialog;

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
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.ItemClickListener;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditRelationalModelPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TreeGrid<AbstractNamedObject> treeGrid = new TreeGrid<AbstractNamedObject>();

    Grid<Record> grid = new Grid<Record>();

    RelationalModel model;

    Set<AbstractNamedObject> lastEditItemIds = Collections.emptySet();

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
    
    Registration enterKeyRegistration;

    boolean readOnly;

    public EditRelationalModelPanel(ApplicationContext context, String modelId, boolean readOnly) {
        this.context = context;
        this.model = new RelationalModel(modelId);
        this.readOnly = readOnly;
        context.getConfigurationService().refresh(model);

        ButtonBar buttonBar1 = new ButtonBar();
        addComponent(buttonBar1);
        ButtonBar buttonBar2 = new ButtonBar();
        addComponent(buttonBar2);

            addEntityButton = buttonBar1.addButton("Add Entity", VaadinIcons.TABLE);
            addEntityButton.addClickListener(new AddEntityClickListener());

            addAttributeButton = buttonBar1.addButton("Add Attr", VaadinIcons.SPLIT_H);
            addAttributeButton.addClickListener(new AddAttributeClickListener());

            editButton = buttonBar1.addButton("Edit", VaadinIcons.EDIT);
            editButton.addClickListener(new EditClickListener());

            removeButton = buttonBar1.addButton("Remove", VaadinIcons.TRASH);
            removeButton.addClickListener(new RemoveClickListener());

            moveUpButton = buttonBar2.addButton("Up", VaadinIcons.ARROW_UP, e -> moveUp());
            moveDownButton = buttonBar2.addButton("Down", VaadinIcons.ARROW_DOWN, e -> moveDown());
            moveTopButton = buttonBar2.addButton("Top", VaadinIcons.ANGLE_DOUBLE_UP,
                    e -> moveTop());
            moveBottomButton = buttonBar2.addButton("Bottom", VaadinIcons.ANGLE_DOUBLE_DOWN,
                    e -> moveBottom());

            importButton = buttonBar1.addButtonRight("Import ...", VaadinIcons.UPLOAD,
                    new ImportClickListener());

        buttonBar1.addButtonRight("Export...", VaadinIcons.DOWNLOAD, (e) -> export());

        filterField = buttonBar2.addFilter();
        filterField.addValueChangeListener(new ValueChangeListener<String>() {
            public void valueChange(ValueChangeEvent<String> event) {
                filterField.setValue(event.getValue());
                treeGrid.getTreeData().clear();
                addAll(event.getValue(), EditRelationalModelPanel.this.model.getModelEntities());
            }
        });

        treeGrid.setSizeFull();
        //treeGrid.setCacheRate(100);
        //treeGrid.setPageLength(100);
        treeGrid.setSelectionMode(SelectionMode.MULTI);
        treeGrid.addComponentColumn(obj -> {
            if (lastEditItemIds.contains(obj) && !readOnly) {
                TextField t = new TextField();
                t.setValueChangeMode(ValueChangeMode.LAZY);
                t.setValueChangeTimeout(200);
                t.addValueChangeListener(event -> {
                    String newName = trim(event.getValue());
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
                });
                t.setWidth(100, Unit.PERCENTAGE);
                t.setValue(obj.getName());
                t.focus();
                t.selectAll();
                t.setIcon(obj instanceof ModelEntity ? VaadinIcons.TABLE : VaadinIcons.SPLIT_H);
                return t;
            } else {
                Label label = UiUtils.getName(filterField.getValue(), obj.getName());
                label.setIcon(obj instanceof ModelEntity ? VaadinIcons.TABLE : VaadinIcons.SPLIT_H);
                return label;
            }
        }).setCaption("Name");

        treeGrid.addComponentColumn(itemId -> {
            if (itemId instanceof ModelAttrib) {
                final ModelAttrib obj = (ModelAttrib) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    TextField t = new TextField();
                    t.setValueChangeMode(ValueChangeMode.LAZY);
                    t.setValueChangeTimeout(200);
                    t.addValueChangeListener(event -> {
                        obj.setDescription(trim(event.getValue()));
                        EditRelationalModelPanel.this.context.getConfigurationService().save(obj);
                    });
                    t.setWidth(100, Unit.PERCENTAGE);
                    t.setValue(obj.getDescription() != null ? obj.getDescription() : "");
                    return t;
                } else {
                    return UiUtils.getName(filterField.getValue(), obj.getDescription());
                }
            }
            if (itemId instanceof ModelEntity) {
                final ModelEntity obj = (ModelEntity) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    TextField t = new TextField();
                    t.setValueChangeMode(ValueChangeMode.LAZY);
                    t.setValueChangeTimeout(200);
                    t.addValueChangeListener(event -> {
                        obj.setDescription(trim(event.getValue()));
                        EditRelationalModelPanel.this.context.getConfigurationService().save(obj);
                    });
                    t.setWidth(100, Unit.PERCENTAGE);
                    t.setValue(obj.getDescription() != null ? obj.getDescription() : "");
                    return t;
                } else {
                    return UiUtils.getName(filterField.getValue(), obj.getDescription());
                }
            } else
                return null;
        }).setCaption("Description");

        treeGrid.addComponentColumn(itemId -> {
            if (itemId instanceof ModelAttrib) {
                final ModelAttrib obj = (ModelAttrib) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    final ComboBox<String> cbox = new ComboBox<String>();
                    cbox.setEmptySelectionAllowed(false);
                    List<String> itemList = new ArrayList<String>();
                    for (DataType dataType : DataType.values()) {
                        itemList.add(dataType.name());
                    }
                    cbox.setItems(itemList);
                    cbox.setValue(obj.getType());
                    cbox.addValueChangeListener(new ValueChangeListener<String>() {
                        public void valueChange(ValueChangeEvent<String> event) {
                            obj.setType(cbox.getValue());
                            EditRelationalModelPanel.this.context.getConfigurationService().save(obj);
                        }
                    });
                    cbox.addBlurListener(new BlurListener() {
                        public void blur(BlurEvent event) {
                            List<AbstractNamedObject> items = getAllItems();
                            boolean found = false;
                            for (AbstractNamedObject item : items) {
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
                    return new Label(obj.getType());
                }
            } else {
                return null;
            }
        }).setCaption("Type");

        treeGrid.addComponentColumn(itemId -> {
            if (itemId instanceof ModelAttrib) {
                final ModelAttrib obj = (ModelAttrib) itemId;
                if (lastEditItemIds.contains(itemId) && !readOnly) {
                    final CheckBox cbox = new CheckBox();
                    cbox.setValue(obj.isPk());
                    cbox.addValueChangeListener(event -> togglePk(obj));
                    cbox.addBlurListener(new BlurListener() {
                        public void blur(BlurEvent event) {
                            List<AbstractNamedObject> items = getAllItems();
                            boolean found = false;
                            for (AbstractNamedObject item : items) {
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
                    Label label = new Label(VaadinIcons.KEY.getHtml());
                    label.setContentMode(ContentMode.HTML);
                    return label;
                }
            }
            return null;
        }).setCaption("PK").setWidth(40);

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

        grid.addColumn(Record::getEntityName).setCaption("Entity Name");
        grid.addColumn(Record::getAttributeName).setCaption("Attribute Name");
        grid.addColumn(Record::getDescription).setCaption("Description");
        grid.addColumn(Record::getType).setCaption("Type");
        grid.addColumn(Record::getPk).setCaption("PK");

        if (model.getModelEntities().size() > 10) {
            collapseAll();
        }
    }

    protected List<AbstractNamedObject> getAllItems() {
        List<AbstractNamedObject> itemList = new ArrayList<AbstractNamedObject>();
        addItemsRecursively(null, itemList);
        return itemList;
    }
    
    protected void addItemsRecursively(AbstractNamedObject item, List<AbstractNamedObject> list) {
        if (item != null) {
            list.add(item);
        }
        for (AbstractNamedObject child : treeGrid.getTreeData().getChildren(item)) {
            addItemsRecursively(child, list);
        }
    }

    protected void move(boolean down, boolean toEnd) {
        ModelAttrib selected = (ModelAttrib) getSelected();
        if (selected != null) {
            ModelEntity entity = (ModelEntity) treeGrid.getTreeData().getParent(selected);
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

            List<AbstractNamedObject> children = new ArrayList<AbstractNamedObject>(treeGrid.getTreeData().getChildren(entity));
            for (AbstractNamedObject object : children) {
                treeGrid.getTreeData().removeItem(object);
            }

            for (ModelAttrib modelAttribute : attributes) {
                addModelAttribute(entity, modelAttribute);
            }
            treeGrid.select(selected);

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
        for (AbstractNamedObject itemId : getAllItems()) {
            treeGrid.collapse(itemId);
        }
    }

    protected void expandAll() {
        for (AbstractNamedObject itemId : getAllItems()) {
            treeGrid.expand(itemId);
        }
    }

    protected void export() {
        updateExportGrid(filterField.getValue(), model.getModelEntities());
        ExportDialog.show(context, grid);
    }

    protected void togglePk(ModelAttrib a) {
        a.setPk(!a.isPk());
        context.getConfigurationService().save(a);
    }

    public void setButtonsEnabled() {
        if (!readOnly) {
            Set<AbstractNamedObject> selected = getSelectedItems();
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

    protected Set<AbstractNamedObject> getSelectedItems() {
        return treeGrid.getSelectedItems();
    }

    protected void selectOnly(AbstractNamedObject itemId) {
        treeGrid.deselectAll();
        treeGrid.select(itemId);
    }

    protected void add(ModelEntity modelEntity) {
        addModelEntity(modelEntity);
        for (ModelAttrib modelAttribute : modelEntity.getModelAttributes()) {
            modelAttribute.setEntityId(modelEntity.getId());
            addModelAttribute(modelEntity, modelAttribute);
        }
        treeGrid.expand(modelEntity);
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
        treeGrid.getDataProvider().refreshAll();
    }

    protected void updateExportGrid(String filter, Collection<ModelEntity> modelEntityList) {
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

        List<Record> recordList = new ArrayList<Record>();
        Collections.sort(filteredModelEntityList, new ModelEntitySorter());
        for (ModelEntity modelEntity : filteredModelEntityList) {
            for (ModelAttrib modelAttribute : modelEntity.getModelAttributes()) {
                recordList.add(new Record(modelEntity, modelAttribute));
            }
        }
        grid.setItems(recordList);
    }

    protected void addModelEntity(ModelEntity modelEntity) {
        treeGrid.getTreeData().addItem(null, modelEntity);
    }

    protected void addModelAttribute(ModelEntity entity, ModelAttrib modelAttribute) {
        treeGrid.getTreeData().addItem(entity, modelAttribute);
    }

    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        treeGrid.getDataProvider().refreshAll();
        grid.getDataProvider().refreshAll();
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
            Set<AbstractNamedObject> itemIds = getSelectedItems();
            if (itemIds.size() > 0) {
                ModelAttrib a = new ModelAttrib();
                a.setName("New Attribute");
                a.setDataType(DataType.VARCHAR);
                AbstractNamedObject itemId = itemIds.iterator().next();
                ModelEntity entity = null;
                if (itemId instanceof ModelEntity) {
                    entity = (ModelEntity) itemId;
                } else if (itemId instanceof ModelAttrib) {
                    entity = (ModelEntity) treeGrid.getTreeData().getParent(itemId);
                }

                if (entity != null) {
                    a.setEntityId(entity.getId());
                    entity.addModelAttribute(a);
                    context.getConfigurationService().save(a);
                    addModelAttribute(entity, a);
                    treeGrid.expand(entity);
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

            Set<AbstractNamedObject> itemIds = new HashSet<AbstractNamedObject>();
            Set<AbstractNamedObject> selectedIds = getSelectedItems();
            
            ConfirmDialog.show("Delete?",
                    "Are you sure you want to delete the " + selectedIds.size() + " selected items?",
                    ()->{
                        for (AbstractNamedObject itemId : selectedIds) {
                            Collection<AbstractNamedObject> children = treeGrid.getTreeData().getChildren(itemId);
                            if (children != null) {
                                itemIds.addAll(children);
                            }
                            itemIds.add(itemId);
                        }

                        for (AbstractNamedObject itemId : itemIds) {
                            if (itemId instanceof ModelAttrib) {
                                ModelAttrib a = (ModelAttrib) itemId;
                                context.getConfigurationService().delete((ModelAttrib) itemId);
                                ModelEntity entity = (ModelEntity) treeGrid.getTreeData().getParent(itemId);
                                entity.removeModelAttribute(a);
                                treeGrid.getTreeData().removeItem(itemId);
                            }
                        }
                        for (AbstractNamedObject itemId : itemIds) {
                            if (itemId instanceof ModelEntity) {
                                context.getConfigurationService().delete((ModelEntity) itemId);
                                treeGrid.getTreeData().removeItem(itemId);
                                model.getModelEntities().remove(itemId);
                            }
                        }
                        treeGrid.getDataProvider().refreshAll();

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
            for (Object itemId : getAllItems()) {
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
            treeGrid.getDataProvider().refreshAll();
        }
    }

    class TreeGridItemClickListener implements ItemClickListener<AbstractNamedObject> {
        long lastClick;

        public void itemClick(ItemClick<AbstractNamedObject> event) {
            if (event.getMouseEventDetails().isDoubleClick()) {
                editSelectedItem();
            } else if (System.currentTimeMillis() - lastClick > 1000
                    && getSelectedItems().size() > 0) {
                treeGrid.deselectAll();
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TreeGridSelectionListener implements SelectionListener<AbstractNamedObject> {
        public void selectionChange(SelectionEvent<AbstractNamedObject> event) {
            lastEditItemIds = Collections.emptySet();
            treeGrid.getDataProvider().refreshAll();
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
