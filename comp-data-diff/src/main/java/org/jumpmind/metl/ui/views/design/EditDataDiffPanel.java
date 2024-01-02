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
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.components.grid.GridRowDragger;
import com.vaadin.ui.TextField;

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

    EditAttributesWindow attributeWindow;

    Set<EntitySettings> selectedItemIds;

    protected void buildUI() {
        buildButtonBar();
        buildEntityGrid();
        fillEntityContainer();
        updateEntityGrid(null);
        buildAttributeWindow();

    }

    protected void buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
        if (!readOnly) {
            editButton = buttonBar.addButton("Edit Columns", VaadinIcons.EDIT);
            editButton.addClickListener(new EditButtonClickListener());
        }
        entityFilterField = buttonBar.addFilter();
        entityFilterField.addValueChangeListener(event -> updateEntityGrid(event.getValue()));

        if (!readOnly) {
            Button moveUpButton = buttonBar.addButton("Move Up", VaadinIcons.ARROW_UP);
            moveUpButton.addClickListener(new MoveUpClickListener());

            Button moveDownButton = buttonBar.addButton("Move Down", VaadinIcons.ARROW_DOWN);
            moveDownButton.addClickListener(new MoveDownClickListener());

            Button moveTopButton = buttonBar.addButton("Move Top", VaadinIcons.ANGLE_DOUBLE_UP);
            moveTopButton.addClickListener(new MoveTopClickListener());

            Button moveBottomButton = buttonBar.addButton("Move Bottom", VaadinIcons.ANGLE_DOUBLE_DOWN);
            moveBottomButton.addClickListener(new MoveBottomClickListener());

            Button cutButton = buttonBar.addButton("Cut", VaadinIcons.SCISSORS);
            cutButton.addClickListener(new CutClickListener());

            Button pasteButton = buttonBar.addButton("Paste", VaadinIcons.PASTE);
            pasteButton.addClickListener(new PasteClickListener());
        }

        addComponent(buttonBar);
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

    class EditButtonClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;

        public void buttonClick(ClickEvent event) {
            if (getSelectedItem() != null) {
                refreshAttributeContainer((EntitySettings) getSelectedItem());
                updateAttributeGrid();
                attributeWindow.show();
            }
        }
    }

    protected void buildEntityGrid() {
        entityGrid.setSizeFull();
        entityGrid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelEntity entity = model.getEntityById(setting.getEntityId());
            return UiUtils.getName(entityFilterField.getValue(), entity.getName());
        }).setCaption("Entity Name").setWidth(250).setExpandRatio(1).setSortable(false);
        entityGrid.addComponentColumn(setting -> createEntityCheckBox(setting, DataDiff.ENTITY_ADD_ENABLED))
                .setCaption("Add Enabled").setSortable(false);
        entityGrid.addComponentColumn(setting -> createEntityCheckBox(setting, DataDiff.ENTITY_CHG_ENABLED))
                .setCaption("Chg Enabled").setSortable(false);
        entityGrid.addComponentColumn(setting -> createEntityCheckBox(setting, DataDiff.ENTITY_DEL_ENABLED))
                .setCaption("Del Enabled").setSortable(false);
        entityGrid.setSelectionMode(SelectionMode.MULTI);
        if (!readOnly) {
            new GridRowDragger<EntitySettings>(entityGrid);
        }
        addComponent(entityGrid);
        setExpandRatio(entityGrid, 1.0f);
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
                    return new Integer(o1.getOrdinalSetting()).compareTo(new Integer(o2.getOrdinalSetting()));
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

    protected CheckBox createEntityCheckBox(final EntitySettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.addValueChangeListener(new ValueChangeListener<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent<Boolean> event) {
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
        checkBox.setReadOnly(readOnly);
        return checkBox;
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

    // attribute window and support

    protected void buildAttributeWindow() {
        attributeWindow = new EditAttributesWindow();
    }

    class EditAttributesWindow extends ResizableWindow {
        private static final long serialVersionUID = 1L;

        public EditAttributesWindow() {
            super("Edit Columns to Compare");
            setWidth(800f, Unit.PIXELS);
            setHeight(600f, Unit.PIXELS);
            content.setMargin(true);
            buildAttributeGrid();
            addComponent(buildButtonFooter(buildCloseButton()));
        }

        private void buildAttributeGrid() {

            attributeGrid.setSizeFull();
            attributeGrid.addColumn(setting -> {
                RelationalModel model = (RelationalModel) component.getInputModel();
                ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
                return UiUtils.getName(entityFilterField.getValue(), attribute.getName());
            }).setCaption("Attribute Name").setWidth(250).setExpandRatio(1).setSortable(false);
            attributeGrid.addComponentColumn(setting -> createAttributeCheckBox(setting, DataDiff.ATTRIBUTE_COMPARE_ENABLED))
                    .setCaption("Compare Enabled").setSortable(false);
            addComponent(attributeGrid, 1);

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

    protected CheckBox createAttributeCheckBox(final AttributeSettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        if (settings.isPrimaryKey()) {
            checkBox.setEnabled(false);
        }
        checkBox.addValueChangeListener(new ValueChangeListener<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent<Boolean> event) {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(settings.getAttributeId(), key);

                String oldValue = setting == null ? Boolean.TRUE.toString() : setting.getValue();
                if (setting == null) {
                    setting = new ComponentAttribSetting(settings.getAttributeId(), component.getId(), key, Boolean.TRUE.toString());
                    component.addAttributeSetting(setting);
                }
                setting.setValue(checkBox.getValue().toString());
                if (!oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
        checkBox.setReadOnly(readOnly);
        return checkBox;
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

    class MoveUpClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                EntitySettings firstItem = itemIds.iterator().next();
                int index = filteredEntitySettings.indexOf(firstItem) - 1;
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
                int index = filteredEntitySettings.indexOf(lastItem) + 1;
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
            moveItemsTo(getSelectedItems(), filteredEntitySettings.size() - 1);
        }
    }

    class CutClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            selectedItemIds = new LinkedHashSet<EntitySettings>(getSelectedItems());
            entityGrid.deselectAll();
        }
    }

    class PasteClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && selectedItemIds != null) {
                int index = filteredEntitySettings.indexOf(itemIds.iterator().next());
                moveItemsTo(selectedItemIds, index);
                selectedItemIds = null;
            }
        }
    }
}
