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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.DataDiff;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EditDataDiffPanel extends AbstractComponentEditPanel {

    Table table = new Table();

    TextField filterField;

    List<EntitySettings> entitySettings = new ArrayList<EntitySettings>();

    BeanItemContainer<EntitySettings> container = new BeanItemContainer<EntitySettings>(EntitySettings.class);
    
    Set<EntitySettings> selectedItemIds;

    protected void buildUI() {

        ButtonBar buttonBar = new ButtonBar();
        Button moveUpButton = buttonBar.addButton("Move Up", FontAwesome.ARROW_UP);
        moveUpButton.addClickListener(new MoveUpClickListener());

        Button moveDownButton = buttonBar.addButton("Move Down", FontAwesome.ARROW_DOWN);
        moveDownButton.addClickListener(new MoveDownClickListener());

        Button moveTopButton = buttonBar.addButton("Move Top", FontAwesome.ANGLE_DOUBLE_UP);
        moveTopButton.addClickListener(new MoveTopClickListener());

        Button moveBottomButton = buttonBar.addButton("Move Bottom", FontAwesome.ANGLE_DOUBLE_DOWN);
        moveBottomButton.addClickListener(new MoveBottomClickListener());

        Button cutButton = buttonBar.addButton("Cut", FontAwesome.CUT);
        cutButton.addClickListener(new CutClickListener());

        Button pasteButton = buttonBar.addButton("Paste", FontAwesome.PASTE);
        pasteButton.addClickListener(new PasteClickListener());
        addComponent(buttonBar);

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(event -> updateTable(event.getText()));
        addComponent(buttonBar);

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSortEnabled(false);
        table.setSizeFull();
        table.addGeneratedColumn("entityName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                EntitySettings setting = (EntitySettings) itemId;
                Model model = component.getInputModel();
                ModelEntity entity = model.getEntityById(setting.getEntityId());
                return UiUtils.getName(filterField.getValue(), entity.getName());
            }
        });
        table.setVisibleColumns(new Object[] { "entityName", "addEnabled", "updateEnabled", "deleteEnabled" });
        table.setColumnWidth("entityName", 250);
        table.setColumnWidth("attributeName", 250);
        table.setColumnHeaders(new String[] { "Entity Name", "Add Enabled", "Chg Enabled", "Del Enabled" });
        table.setColumnExpandRatio("entityName", 1);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setMultiSelect(true);
        table.setDragMode(TableDragMode.MULTIROW);
        table.setDropHandler(new TableDropHandler());
        table.setEditable(true);
        addComponent(table);
        setExpandRatio(table, 1.0f);

        if (component.getInputModel() != null) {

            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                ComponentEntitySetting insert = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_ADD_ENABLED);
                ComponentEntitySetting update = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_CHG_ENABLED);
                ComponentEntitySetting delete = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_DEL_ENABLED);
                ComponentEntitySetting order = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_ORDER);
                boolean insertEnabled = insert != null ? Boolean.parseBoolean(insert.getValue()) : true;
                boolean updateEnabled = update != null ? Boolean.parseBoolean(update.getValue()) : true;
                boolean deleteEnabled = delete != null ? Boolean.parseBoolean(delete.getValue()) : true;
                int orderValue = order != null ? Integer.parseInt(order.getValue()) : 0;
                entitySettings.add(new EntitySettings(entity.getId(), insertEnabled, updateEnabled, deleteEnabled, orderValue));
            }

            Collections.sort(entitySettings, new Comparator<EntitySettings>() {
                @Override
                public int compare(EntitySettings o1, EntitySettings o2) {
                    return new Integer(o1.getOrder()).compareTo(new Integer(o2.getOrder()));
                }
            });
        }

        updateTable(null);

    }

    protected void updateTable(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        filterField.setValue(filter);
        table.removeAllItems();
        for (EntitySettings entitySetting : entitySettings) {
            Model model = component.getInputModel();
            ModelEntity entity = model.getEntityById(entitySetting.getEntityId());
            if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)) {
                table.addItem(entitySetting);
            }
        }
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final EntitySettings settings = (EntitySettings) itemId;

            if (propertyId.equals("addEnabled")) {
                return createCheckBox(settings, DataDiff.ENTITY_ADD_ENABLED);
            } else if (propertyId.equals("updateEnabled")) {
                return createCheckBox(settings, DataDiff.ENTITY_CHG_ENABLED);
            } else if (propertyId.equals("deleteEnabled")) {
                return createCheckBox(settings, DataDiff.ENTITY_DEL_ENABLED);
            } else {
                return null;
            }
        }
    }

    protected CheckBox createCheckBox(final EntitySettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.setImmediate(true);
        checkBox.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                ComponentEntitySetting setting = component.getSingleEntitySetting(settings.getEntityId(), key);

                String oldValue = setting == null ? Boolean.TRUE.toString() : setting.getValue();
                if (setting == null) {
                    setting = new ComponentEntitySetting(settings.getEntityId(), component.getId(), key, Boolean.TRUE.toString());
                    component.addEntitySetting(setting);
                }
                setting.setValue(checkBox.getValue().toString());
                if (!oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
        return checkBox;

    }

    @SuppressWarnings("unchecked")
    protected Set<EntitySettings> getSelectedItems() {
        return (Set<EntitySettings>) table.getValue();
    }

    protected void moveItemsTo(Set<EntitySettings> itemIds, int index) {
        if (index >= 0 && index < container.getItemIds().size() && itemIds.size() > 0) {
            int firstItemIndex = container.indexOfId(itemIds.iterator().next());
            if (index != firstItemIndex) {
                for (EntitySettings itemId : itemIds) {
                    boolean movingUp = index < container.indexOfId(itemId);
                    container.removeItem(itemId);
                    container.addItemAt(index, itemId);
                    if (movingUp) {
                        index++;
                    }
                }
                saveOrdinalSettings();
            }
        }
    }

    protected void saveOrdinalSettings() {
        int ordinal = 1;
        boolean needsRefreshed = false;
        for (EntitySettings record : container.getItemIds()) {

            if (record.getOrder() != ordinal) {
                record.setOrder(ordinal);
                needsRefreshed = true;
            }
            saveSetting(record.getEntityId(), DataDiff.ENTITY_ORDER, String.valueOf(ordinal));
            ordinal++;
        }
        if (needsRefreshed) {
            table.refreshRowCache();
        }
    }

    protected void saveSetting(String entityId, String name, String value) {
        ComponentEntitySetting setting = component.getSingleEntitySetting(entityId, name);
        if (setting == null) {
            setting = new ComponentEntitySetting(entityId, name, value);
            setting.setComponentId(component.getId());
            component.addEntitySetting(setting);
            context.getConfigurationService().save(setting);
        } else if (!StringUtils.equals(setting.getValue(), value)) {
            setting.setValue(value);
            context.getConfigurationService().save(setting);
        }
    }

    public static class EntitySettings {

        int order;
        String entityId;
        boolean addEnabled;
        boolean updateEnabled;
        boolean deleteEnabled;

        public EntitySettings(String entityId, boolean insertEnabled, boolean updateEnabled, boolean deleteEnabled, int order) {
            this.entityId = entityId;
            this.addEnabled = insertEnabled;
            this.updateEnabled = updateEnabled;
            this.deleteEnabled = deleteEnabled;
            this.order = order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }

        public void setAddEnabled(boolean insertEnabled) {
            this.addEnabled = insertEnabled;
        }

        public boolean isAddEnabled() {
            return addEnabled;
        }

        public void setUpdateEnabled(boolean updateEnabled) {
            this.updateEnabled = updateEnabled;
        }

        public boolean isUpdateEnabled() {
            return updateEnabled;
        }

        public void setDeleteEnabled(boolean deleteEnabled) {
            this.deleteEnabled = deleteEnabled;
        }

        public boolean isDeleteEnabled() {
            return deleteEnabled;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EntitySettings) {
                return entityId.equals(((EntitySettings) obj).getEntityId());
            } else {
                return super.equals(obj);
            }
        }

        @Override
        public int hashCode() {
            return entityId.hashCode();
        }

    }

    class MoveUpClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                EntitySettings firstItem = itemIds.iterator().next();
                int index = container.indexOfId(firstItem) - 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveDownClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                EntitySettings lastItem = null;
                Iterator<EntitySettings> iter = itemIds.iterator();
                while (iter.hasNext()) {
                    lastItem = iter.next();
                }
                int index = container.indexOfId(lastItem) + 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveTopClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            moveItemsTo(getSelectedItems(), 0);
        }
    }

    class MoveBottomClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            moveItemsTo(getSelectedItems(), container.size() - 1);
        }
    }

    class CutClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            selectedItemIds = new LinkedHashSet<EntitySettings>(itemIds);
            for (EntitySettings itemId : itemIds) {
                table.unselect(itemId);
            }
            table.refreshRowCache();
        }
    }

    class PasteClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && selectedItemIds != null) {
                int index = container.indexOfId(itemIds.iterator().next());
                moveItemsTo(selectedItemIds, index);
                selectedItemIds = null;
            }
        }
    }

    class TableDropHandler implements DropHandler {
        public void drop(DragAndDropEvent event) {
            AbstractSelectTargetDetails targetDetails = (AbstractSelectTargetDetails) event.getTargetDetails();
            Transferable transferable = event.getTransferable();
            if (transferable.getSourceComponent() == table) {
                EntitySettings target = (EntitySettings) targetDetails.getItemIdOver();
                moveItemsTo(getSelectedItems(), container.indexOfId(target));
            }
        }

        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }

}
