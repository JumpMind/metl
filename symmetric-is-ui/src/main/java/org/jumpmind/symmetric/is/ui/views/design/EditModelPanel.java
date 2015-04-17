package org.jumpmind.symmetric.is.ui.views.design;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.DataType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.UiUtils;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

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

    public EditModelPanel(ApplicationContext context, Model model) {
        this.context = context;
        this.model = model;
        
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

            @Override
            public void textChange(TextChangeEvent event) {
                filterField.setValue(event.getText());
                setButtonsEnabled(event.getText());
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

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                final AbstractObject obj = (AbstractObject) itemId;
                if (lastEditItemIds.contains(itemId)) {
                    ImmediateUpdateTextField t = new ImmediateUpdateTextField(null) {
                        protected void save() {
                            obj.setName(getValue());
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

            @Override
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
                            
                            @Override
                            public void valueChange(ValueChangeEvent event) {
                                obj.setType((String)cbox.getValue());
                                EditModelPanel.this.context.getConfigurationService().save(obj);
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

        addComponent(treeTable);
        setExpandRatio(treeTable, 1.0f);
        addAll("", model.getModelEntities());
        setButtonsEnabled("");
    }

    public void setButtonsEnabled(String filter) {
        boolean noFilter = isBlank(filter);
        Set<Object> selected = getSelectedItems();
        addEntityButton.setEnabled(noFilter);
        importButton.setEnabled(noFilter);
        addAttributeButton.setEnabled(noFilter && selected.size() > 0);
        removeButton.setEnabled(selected.size() > 0);
        editButton.setEnabled(selected.size() > 0);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
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

    protected void add(String filter, ModelEntity modelEntity) {
        addModelEntity(filter, modelEntity);
        for (ModelAttribute modelAttribute : modelEntity.getModelAttributes()) {
            treeTable.setChildrenAllowed(modelEntity, true);
            modelAttribute.setEntityId(modelEntity.getId());
            addModelAttribute(filter, modelEntity, modelAttribute);
        }
        treeTable.setCollapsed(modelEntity, false);
    }

    protected void addAll(String filter, Collection<ModelEntity> modelEntityList) {
        for (ModelEntity modelEntity : modelEntityList) {
            boolean add = isBlank(filter) || modelEntity.getName().contains(filter);
            if (!add) {
                for (ModelAttribute modelAttribute : modelEntity.getModelAttributes()) {
                    add |= modelAttribute.getName().contains(filter);
                }
            }

            if (add) {
                add(filter, modelEntity);
            }
        }
    }

    protected void addModelEntity(String filter, ModelEntity modelEntity) {
        treeTable.addItem(modelEntity);
        treeTable.setItemIcon(modelEntity, FontAwesome.TABLE);
        treeTable.setChildrenAllowed(modelEntity, false);
    }

    protected void addModelAttribute(String filter, ModelEntity entity, ModelAttribute modelAttribute) {
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
            addModelEntity("", e);
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
                    addModelAttribute("", entity, a);
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
                    ModelEntity entity = (ModelEntity)treeTable.getParent(itemId);
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
                    add("", e);
                    model.getModelEntities().add(e);
                } else {
                    for (ModelAttribute a : e.getModelAttributes()) {
                        if (modelEntity.getModelAttributeByName(a.getName()) == null) {
                            a.setEntityId(modelEntity.getId());
                            context.getConfigurationService().save(a);
                            modelEntity.addModelAttribute(a);
                            addModelAttribute("", modelEntity, a);
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
            } else if (System.currentTimeMillis() - lastClick > 1000 && getSelectedItems().size() > 0) {
                treeTable.setValue(null);
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TreeTableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            lastEditItemIds = Collections.emptySet();
            treeTable.refreshRowCache();
            setButtonsEnabled(filterField.getValue());
        }
    }

}
