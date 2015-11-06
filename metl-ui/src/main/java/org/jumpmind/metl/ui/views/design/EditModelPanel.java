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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.util.ModelEntitySorter;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
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
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditModelPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TreeTable treeTable = new TreeTable();

    Model model;

    Set<Object> lastEditItemIds = Collections.emptySet();

    TableColumnSelectWindow tableColumnSelectWindow;

    Button addEntityButton;

    Button addAttributeButton;

    Button editButton;

    Button removeButton;

    Button importButton;
    
    TextField filterField;
    
    ShortcutListener enterKeyListener;

    public EditModelPanel(ApplicationContext context, String modelId) {
        this.context = context;
        this.model = new Model(modelId);
        context.getConfigurationService().refresh(model);

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        addEntityButton = buttonBar.addButton("Add Entity", FontAwesome.TABLE);
        addEntityButton.addClickListener(new AddEntityClickListener());

        addAttributeButton = buttonBar.addButton("Add Attribute", FontAwesome.COLUMNS);
        addAttributeButton.addClickListener(new AddAttributeClickListener());
        
        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());

        importButton = buttonBar.addButton("Import ...", FontAwesome.DOWNLOAD);
        importButton.addClickListener(new ImportClickListener());

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                filterField.setValue(event.getText());
                treeTable.removeAllItems();
                addAll(event.getText(), EditModelPanel.this.model.getModelEntities());
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
                final AbstractObject obj = (AbstractObject) itemId;
                if (lastEditItemIds.contains(itemId)) {
                    ImmediateUpdateTextField t = new ImmediateUpdateTextField(null) {
                        protected void save(String text) {
                            obj.setName(text);
                            EditModelPanel.this.context.getConfigurationService().save(obj);
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

        treeTable.addGeneratedColumn("type", new ColumnGenerator() {
            public Object generateCell(Table source, Object itemId, Object columnId) {
                if (itemId instanceof ModelAttribute) {
                    final ModelAttribute obj = (ModelAttribute) itemId;
                    if (lastEditItemIds.contains(itemId)) {
                        final ComboBox cbox = new ComboBox();
                        cbox.setNullSelectionAllowed(false);
                        for (DataType dataType : DataType.values()) {
                            cbox.addItem(dataType.name());
                        }
                        cbox.setValue(obj.getType());
                        cbox.addValueChangeListener(new ValueChangeListener() {
                            public void valueChange(ValueChangeEvent event) {
                                obj.setType((String) cbox.getValue());
                                EditModelPanel.this.context.getConfigurationService().save(obj);
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
                if (itemId instanceof ModelAttribute) {
                    final ModelAttribute obj = (ModelAttribute) itemId;
                    if (lastEditItemIds.contains(itemId)) {
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

        Button selectAllLink = new Button("Collapse All");
        selectAllLink.addStyleName(ValoTheme.BUTTON_LINK);
        selectAllLink.addStyleName(ValoTheme.BUTTON_SMALL);
        hlayout.addComponent(selectAllLink);
        selectAllLink.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                for (Object itemId : treeTable.getItemIds()) {
                    treeTable.setCollapsed(itemId, true);
                }
            }
        });

        Button selectNoneLink = new Button("Expand All");
        selectNoneLink.addStyleName(ValoTheme.BUTTON_LINK);
        selectNoneLink.addStyleName(ValoTheme.BUTTON_SMALL);
        hlayout.addComponent(selectNoneLink);
        selectNoneLink.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                for (Object itemId : treeTable.getItemIds()) {
                    treeTable.setCollapsed(itemId, false);
                }
            }
        });
        
        addAll("", model.getModelEntities());
        setButtonsEnabled();
    }
    
    protected void togglePk(ModelAttribute a) {
        a.setPk(!a.isPk());
        context.getConfigurationService().save(a);
    }
    
    public void setButtonsEnabled() {
        Set<Object> selected = getSelectedItems();
        addAttributeButton.setEnabled(selected.size() > 0);
        removeButton.setEnabled(selected.size() > 0);
        editButton.setEnabled(selected.size() > 0);
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
        for (ModelAttribute modelAttribute : modelEntity.getModelAttributes()) {
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
                for (ModelAttribute modelAttribute : modelEntity.getModelAttributes()) {
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

    protected void addModelEntity(ModelEntity modelEntity) {
        treeTable.addItem(modelEntity);
        treeTable.setItemIcon(modelEntity, FontAwesome.TABLE);
        treeTable.setChildrenAllowed(modelEntity, false);
    }

    protected void addModelAttribute(ModelEntity entity, ModelAttribute modelAttribute) {
        treeTable.addItem(modelAttribute);
        treeTable.setItemIcon(modelAttribute, FontAwesome.COLUMNS);
        treeTable.setChildrenAllowed(entity, true);
        treeTable.setParent(modelAttribute, entity);
        treeTable.setChildrenAllowed(modelAttribute, false);
    }

    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        treeTable.refreshRowCache();
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
                ModelAttribute a = new ModelAttribute();
                a.setName("New Attribute");
                a.setDataType(DataType.VARCHAR);
                Object itemId = itemIds.iterator().next();
                ModelEntity entity = null;
                if (itemId instanceof ModelEntity) {
                    entity = (ModelEntity) itemId;
                } else if (itemId instanceof ModelAttribute) {
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

            for (Object itemId : selectedIds) {
                Collection<Object> children = (Collection<Object>) treeTable
                        .getContainerDataSource().getChildren(itemId);
                if (children != null) {
                    itemIds.addAll(children);
                }
                itemIds.add(itemId);
            }

            for (Object itemId : itemIds) {
                if (itemId instanceof ModelAttribute) {
                    ModelAttribute a = (ModelAttribute) itemId;
                    context.getConfigurationService().delete((ModelAttribute) itemId);
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
                    for (ModelAttribute a : e.getModelAttributes()) {
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

}
