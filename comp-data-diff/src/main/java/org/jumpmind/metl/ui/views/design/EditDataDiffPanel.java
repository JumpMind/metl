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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.DataDiff;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.ResizableDialog;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class EditDataDiffPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;

    Grid<EntitySettings> entityGrid = new Grid<EntitySettings>();
    List<EntitySettings> entitySettings = new ArrayList<EntitySettings>();
    List<EntitySettings> filteredEntitySettings = new ArrayList<EntitySettings>();

    Grid<AttributeSettings> attributeGrid = new Grid<AttributeSettings>();
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    TextField entityFilterField;

    Button editButton;

    EditAttributesDialog attributeDialog;

    Set<EntitySettings> selectedItemIds;

    protected void buildUI() {
        buildButtonBar();
        buildEntityGrid();
        fillEntityContainer();
        updateEntityGrid(null);
        buildAttributeDialog();

    }

    protected void buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            editButton = buttonBar.addButton("Edit Columns", VaadinIcon.EDIT);
            editButton.addClickListener(new EditButtonClickListener());
        }
        entityFilterField = buttonBar.addFilter();
        entityFilterField.addValueChangeListener(event -> updateEntityGrid(event.getValue()));

        if (!readOnly) {
            Button moveUpButton = buttonBar.addButton("Move Up", VaadinIcon.ARROW_UP);
            moveUpButton.addClickListener(new MoveUpClickListener());

            Button moveDownButton = buttonBar.addButton("Move Down", VaadinIcon.ARROW_DOWN);
            moveDownButton.addClickListener(new MoveDownClickListener());

            Button moveTopButton = buttonBar.addButton("Move Top", VaadinIcon.ANGLE_DOUBLE_UP);
            moveTopButton.addClickListener(new MoveTopClickListener());

            Button moveBottomButton = buttonBar.addButton("Move Bottom", VaadinIcon.ANGLE_DOUBLE_DOWN);
            moveBottomButton.addClickListener(new MoveBottomClickListener());

            Button cutButton = buttonBar.addButton("Cut", VaadinIcon.SCISSORS);
            cutButton.addClickListener(new CutClickListener());

            Button pasteButton = buttonBar.addButton("Paste", VaadinIcon.PASTE);
            pasteButton.addClickListener(new PasteClickListener());
        }

        add(buttonBar);
    }

    protected Set<EntitySettings> getSelectedItems() {
        return entityGrid.getSelectedItems();
    }

    protected EntitySettings getSelectedItem() {
        Set<EntitySettings> selectedItems = getSelectedItems();
        if (selectedItems != null && selectedItems.size() == 1) {
            return selectedItems.iterator().next();
        }
        return null;
    }

    class EditButtonClickListener implements ComponentEventListener<ClickEvent<Button>> {
        private static final long serialVersionUID = 1L;

        public void onComponentEvent(ClickEvent<Button> event) {
            if (getSelectedItem() != null) {
                refreshAttributeContainer((EntitySettings) getSelectedItem());
                updateAttributeGrid();
                attributeDialog.show();
            }
        }
    }

    protected void buildEntityGrid() {
        entityGrid.setSizeFull();
        entityGrid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelEntity entity = model.getEntityById(setting.getEntityId());
            return UiUtils.getName(entityFilterField.getValue(), entity.getName());
        }).setHeader("Entity Name").setWidth("250px").setFlexGrow(1).setSortable(false);
        entityGrid.addComponentColumn(setting -> createEntityCheckbox(setting, DataDiff.ENTITY_ADD_ENABLED))
                .setHeader("Add Enabled").setSortable(false);
        entityGrid.addComponentColumn(setting -> createEntityCheckbox(setting, DataDiff.ENTITY_CHG_ENABLED))
                .setHeader("Chg Enabled").setSortable(false);
        entityGrid.addComponentColumn(setting -> createEntityCheckbox(setting, DataDiff.ENTITY_DEL_ENABLED))
                .setHeader("Del Enabled").setSortable(false);
        entityGrid.setSelectionMode(SelectionMode.MULTI);
        if (!readOnly) {
            entityGrid.setRowsDraggable(true);
        }
        add(entityGrid);
        expand(entityGrid);
    }

    protected void fillEntityContainer() {
        if (component.getInputModel() != null) {

            for (ModelEntity entity : ((RelationalModel)component.getInputModel()).getModelEntities()) {
                ComponentEntitySetting insert = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_ADD_ENABLED);
                ComponentEntitySetting update = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_CHG_ENABLED);
                ComponentEntitySetting delete = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_DEL_ENABLED);
                ComponentEntitySetting ordinal = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_ORDER);
                boolean insertEnabled = insert != null ? Boolean.parseBoolean(insert.getValue()) : true;
                boolean updateEnabled = update != null ? Boolean.parseBoolean(update.getValue()) : true;
                boolean deleteEnabled = delete != null ? Boolean.parseBoolean(delete.getValue()) : true;
                Integer order = ordinal != null ? Integer.parseInt(ordinal.getValue()) : 0;
                entitySettings.add(new EntitySettings(entity.getId(), insertEnabled, updateEnabled, deleteEnabled, order));
            }

            Collections.sort(entitySettings, new Comparator<EntitySettings>() {
                public int compare(EntitySettings o1, EntitySettings o2) {
                    return Integer.valueOf(o1.getOrdinalSetting()).compareTo(Integer.valueOf(o2.getOrdinalSetting()));
                }
            });
        }
    }

    protected void updateEntityGrid(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        entityFilterField.setValue(filter);
        filteredEntitySettings.clear();
        for (EntitySettings entitySetting : entitySettings) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelEntity entity = model.getEntityById(entitySetting.getEntityId());
            if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)) {
                filteredEntitySettings.add(entitySetting);
            }
        }
        entityGrid.setItems(filteredEntitySettings);
    }

    protected void moveItemsTo(Set<EntitySettings> itemIds, int index) {
        if (index >= 0 && index < filteredEntitySettings.size() && itemIds.size() > 0) {
            int firstItemIndex = filteredEntitySettings.indexOf(itemIds.iterator().next());
            if (index != firstItemIndex) {
                for (EntitySettings itemId : itemIds) {
                    boolean movingUp = index < filteredEntitySettings.indexOf(itemId);
                    filteredEntitySettings.remove(itemId);
                    filteredEntitySettings.add(index, itemId);
                    if (movingUp) {
                        index++;
                    }
                }
                calculatePositions();
                saveOrdinalSettings();
            }
        }
    }

    protected void saveOrdinalSettings() {
        String attrName = DataDiff.ENTITY_ORDER;
        int ordinal = 1;
        for (EntitySettings record : filteredEntitySettings) {
            saveSetting(record.getEntityId(), attrName, String.valueOf(ordinal));
            ordinal++;
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

    protected void calculatePositions() {
        boolean needsRefreshed = false;

        int ordinal = 1;
        for (EntitySettings record : filteredEntitySettings) {
            if (record.getOrdinalSetting() != ordinal) {
                record.setOrdinalSetting(ordinal);
                needsRefreshed = true;
            }
            ordinal++;
        }
        if (needsRefreshed) {
            entityGrid.setItems(filteredEntitySettings);
        }
    }

    protected Checkbox createEntityCheckbox(final EntitySettings settings, final String key) {
        final Checkbox checkbox = new Checkbox();
        checkbox.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<Boolean>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(ValueChangeEvent<Boolean> event) {
                ComponentEntitySetting setting = component.getSingleEntitySetting(settings.getEntityId(), key);

                String oldValue = setting == null ? Boolean.TRUE.toString() : setting.getValue();
                if (setting == null) {
                    setting = new ComponentEntitySetting(settings.getEntityId(), component.getId(), key, Boolean.TRUE.toString());
                    component.addEntitySetting(setting);
                }
                setting.setValue(checkbox.getValue().toString());
                if (!oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
        checkbox.setReadOnly(readOnly);
        return checkbox;
    }

    public static class EntitySettings implements Serializable {
        private static final long serialVersionUID = 1L;
        String entityId;
        boolean addEnabled;
        boolean updateEnabled;
        boolean deleteEnabled;
        int ordinalSetting;

        public EntitySettings(String entityId, boolean insertEnabled, boolean updateEnabled, boolean deleteEnabled, int ordinal) {
            this.entityId = entityId;
            this.addEnabled = insertEnabled;
            this.updateEnabled = updateEnabled;
            this.deleteEnabled = deleteEnabled;
            this.ordinalSetting = ordinal;
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

        public int getOrdinalSetting() {
            return ordinalSetting;
        }

        public void setOrdinalSetting(int ordinalSetting) {
            this.ordinalSetting = ordinalSetting;
        }
    }

    // attribute dialog and support

    protected void buildAttributeDialog() {
        attributeDialog = new EditAttributesDialog();
    }

    class EditAttributesDialog extends ResizableDialog {
        private static final long serialVersionUID = 1L;

        public EditAttributesDialog() {
            super("Edit Columns to Compare");
            setWidth("800px");
            setHeight("600px");
            innerContent.setMargin(true);
            buildAttributeGrid();
            add(buildButtonFooter(buildCloseButton()));
        }

        private void buildAttributeGrid() {

            attributeGrid.setSizeFull();
            attributeGrid.addColumn(setting -> {
                RelationalModel model = (RelationalModel) component.getInputModel();
                ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
                return UiUtils.getName(entityFilterField.getValue(), attribute.getName());
            }).setHeader("Attribute Name").setWidth("250px").setFlexGrow(1).setSortable(false);
            attributeGrid.addComponentColumn(setting -> createAttributeCheckbox(setting, DataDiff.ATTRIBUTE_COMPARE_ENABLED))
                    .setHeader("Compare Enabled").setSortable(false);
            add(attributeGrid, 1);

        }
    }

    private void refreshAttributeContainer(EntitySettings selectedRow) {
        attributeSettings.clear();
        ModelEntity entity = ((RelationalModel)component.getInputModel()).getEntityById(selectedRow.getEntityId());
        for (ModelAttrib attribute : entity.getModelAttributes()) {

            ComponentAttribSetting compare = component.getSingleAttributeSetting(attribute.getId(), DataDiff.ATTRIBUTE_COMPARE_ENABLED);
            boolean compareEnabled = compare != null ? Boolean.parseBoolean(compare.getValue()) : true;
            attributeSettings.add(new AttributeSettings(attribute.getId(), compareEnabled, attribute.isPk() == true ? true : false));
        }
    }

    protected void updateAttributeGrid() {
        attributeGrid.setItems(attributeSettings);
    }

    protected Checkbox createAttributeCheckbox(final AttributeSettings settings, final String key) {
        final Checkbox checkbox = new Checkbox();
        if (settings.isPrimaryKey()) {
            checkbox.setEnabled(false);
        }
        checkbox.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<Boolean>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(ValueChangeEvent<Boolean> event) {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(settings.getAttributeId(), key);

                String oldValue = setting == null ? Boolean.TRUE.toString() : setting.getValue();
                if (setting == null) {
                    setting = new ComponentAttribSetting(settings.getAttributeId(), component.getId(), key, Boolean.TRUE.toString());
                    component.addAttributeSetting(setting);
                }
                setting.setValue(checkbox.getValue().toString());
                if (!oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
        checkbox.setReadOnly(readOnly);
        return checkbox;
    }

    public static class AttributeSettings implements Serializable {
        private static final long serialVersionUID = 1L;
        boolean primaryKey;
        String attributeId;
        boolean compareEnabled;

        public AttributeSettings(String attributeId, boolean compareEnabled, boolean primaryKey) {
            this.attributeId = attributeId;
            this.compareEnabled = compareEnabled;
            this.primaryKey = primaryKey;
        }

        public void setCompareEnabled(boolean compareEnabled) {
            this.compareEnabled = compareEnabled;
        }

        public boolean isCompareEnabled() {
            return compareEnabled;
        }

        public String getAttributeId() {
            return attributeId;
        }

        public void setAttributeId(String attributeId) {
            this.attributeId = attributeId;
        }

        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AttributeSettings) {
                return attributeId.equals(((AttributeSettings) obj).getAttributeId());
            } else {
                return super.equals(obj);
            }
        }
    }

    class MoveUpClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                EntitySettings firstItem = itemIds.iterator().next();
                int index = filteredEntitySettings.indexOf(firstItem) - 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveDownClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                EntitySettings lastItem = null;
                Iterator<EntitySettings> iter = itemIds.iterator();
                while (iter.hasNext()) {
                    lastItem = iter.next();
                }
                int index = filteredEntitySettings.indexOf(lastItem) + 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveTopClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            moveItemsTo(getSelectedItems(), 0);
        }
    }

    class MoveBottomClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            moveItemsTo(getSelectedItems(), filteredEntitySettings.size() - 1);
        }
    }

    class CutClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            selectedItemIds = new LinkedHashSet<EntitySettings>(getSelectedItems());
            entityGrid.deselectAll();
        }
    }

    class PasteClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && selectedItemIds != null) {
                int index = filteredEntitySettings.indexOf(itemIds.iterator().next());
                moveItemsTo(selectedItemIds, index);
                selectedItemIds = null;
            }
        }
    }
}
