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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.ExcelFileWriter;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;

@SuppressWarnings("serial")
public class EditExcelWriterPanel extends AbstractComponentEditPanel {
    
    List<RecordFormat> recordFormatList = new ArrayList<RecordFormat>();

    Grid<RecordFormat> grid = new Grid<RecordFormat>();

    Set<RecordFormat> selectedItemIds;

    protected void buildUI() {
        setPadding(false);
        setSpacing(false);
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            add(buttonBar);

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
        
        buttonBar.addButtonRight("Export", VaadinIcon.DOWNLOAD, (e)->export());

        grid.setSizeFull();
        grid.addColumn(RecordFormat::getEntityName).setHeader("Entity Name").setSortable(false);
        grid.addColumn(RecordFormat::getAttributeName).setHeader("Attribute Name").setSortable(false);
        grid.addColumn(RecordFormat::getOrdinalSetting).setHeader("Ordinal").setSortable(false);
        grid.setSelectionMode(SelectionMode.MULTI);
        if (!readOnly) {
            grid.setRowsDraggable(true);
        }
        add(grid);
        expand(grid);

        RelationalModel model = (RelationalModel) component.getInputModel();

        if (model != null) {
            model = context.getConfigurationService().findRelationalModel(model.getId());
            List<RecordFormat> attributes = new ArrayList<RecordFormat>();

            for (ModelEntity entity : model.getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    attributes.add(new RecordFormat(entity, attr));
                }
            }

            Collections.sort(attributes, new Comparator<RecordFormat>() {
                public int compare(RecordFormat o1, RecordFormat o2) {
                    return Integer.valueOf(o1.getOrdinalSetting()).compareTo(Integer.valueOf(o2.getOrdinalSetting()));
                }
            });

            for (RecordFormat recordFormat : attributes) {
                recordFormatList.add(recordFormat);
            }
        }
        grid.setItems(recordFormatList);
        calculatePositions();
        saveOrdinalSettings();
    }

    protected void export() {
        ExportDialog.show(context, grid);
    }
    
    protected Set<RecordFormat> getSelectedItems() {
        return grid.getSelectedItems();
    }

    protected RecordFormat getSelectedItem() {
        Set<RecordFormat> selectedItems = getSelectedItems();
        if (selectedItems != null && selectedItems.size() == 1) {
            return selectedItems.iterator().next();
        }
        return null;
    }

    protected void calculatePositions() {
        boolean needsRefreshed = false;
        int ordinal = 1;
        for (RecordFormat record : recordFormatList) {
            if (record.getOrdinalSetting() != ordinal) {
                record.setOrdinalSetting(ordinal);
                needsRefreshed = true;
            }
            ordinal++;
        }
        if (needsRefreshed) {
            RecordFormat record = getSelectedItem();
            if (record != null) {
                record.setFocusFieldId("transformText");
            }
            grid.setItems(recordFormatList);
        }
    }

    protected void moveItemsTo(Set<RecordFormat> itemIds, int index) {
        if (index >= 0 && index < recordFormatList.size() && itemIds.size() > 0) {
            int firstItemIndex = recordFormatList.indexOf(itemIds.iterator().next());
            if (index != firstItemIndex) {
                for (RecordFormat itemId : itemIds) {
                    boolean movingUp = index < recordFormatList.indexOf(itemId);
                    recordFormatList.remove(itemId);
                    recordFormatList.add(index, itemId);
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
        String attrName;
        attrName = ExcelFileWriter.EXCEL_WRITER_ATTRIBUTE_ORDINAL;
        int ordinal = 1;
        for (RecordFormat record : recordFormatList) {
            saveSetting(record.getAttributeId(), attrName, String.valueOf(ordinal));
            ordinal++;
        }
    }

    protected void saveSetting(String attributeId, String name, String value) {
        ComponentAttribSetting setting = component.getSingleAttributeSetting(attributeId, name);
        if (setting == null) {
            setting = new ComponentAttribSetting(attributeId, name, value);
            setting.setComponentId(component.getId());
            component.addAttributeSetting(setting);
            context.getConfigurationService().save(setting);
        } else if (!StringUtils.equals(setting.getValue(), value)) {
            setting.setValue(value);
            context.getConfigurationService().save(setting);
        }
    }

    class MoveUpClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Set<RecordFormat> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                RecordFormat firstItem = itemIds.iterator().next();
                int index = recordFormatList.indexOf(firstItem) - 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveDownClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Set<RecordFormat> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                RecordFormat lastItem = null;
                Iterator<RecordFormat> iter = itemIds.iterator();
                while (iter.hasNext()) {
                    lastItem = iter.next();
                }
                int index = recordFormatList.indexOf(lastItem) + 1;
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
            moveItemsTo(getSelectedItems(), recordFormatList.size() - 1);
        }
    }

    class CutClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Set<RecordFormat> itemIds = getSelectedItems();
            selectedItemIds = new LinkedHashSet<RecordFormat>(itemIds);
            grid.deselectAll();
            grid.setItems(recordFormatList);
        }
    }

    class PasteClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Set<RecordFormat> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && selectedItemIds != null) {
                int index = recordFormatList.indexOf(itemIds.iterator().next());
                moveItemsTo(selectedItemIds, index);
                selectedItemIds = null;
            }
        }
    }

    public class RecordFormat {
        ModelEntity modelEntity;

        ModelAttrib modelAttribute;

        Object focusFieldId;

        int ordinalSetting;

        public RecordFormat(ModelEntity modelEntity, ModelAttrib modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;
            ComponentAttribSetting setting = component.getSingleAttributeSetting(modelAttribute.getId(),
            		ExcelFileWriter.EXCEL_WRITER_ATTRIBUTE_ORDINAL);
            if (setting != null) {
                this.ordinalSetting = Integer.parseInt(setting.getValue());
            }
        }

        public int hashCode() {
            return modelEntity.hashCode() + modelAttribute.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof RecordFormat) {
                return hashCode() == ((RecordFormat) obj).hashCode();
            }
            return super.equals(obj);
        }

        public String getEntityName() {
            return modelEntity.getName();
        }

        public String getAttributeName() {
            return modelAttribute.getName();
        }

        public String getAttributeId() {
            return modelAttribute.getId();
        }

        public void setFocusFieldId(Object id) {
            this.focusFieldId = id;
        }

        public int getOrdinalSetting() {
            return ordinalSetting;
        }

        public void setOrdinalSetting(int ordinalSetting) {
            this.ordinalSetting = ordinalSetting;
        }
    }
}
