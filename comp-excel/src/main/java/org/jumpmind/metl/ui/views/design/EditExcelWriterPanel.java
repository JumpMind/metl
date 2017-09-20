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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.ExcelFileWriter;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.ExportDialog;

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
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class EditExcelWriterPanel extends AbstractComponentEditPanel {

    Table table = new Table();

    BeanItemContainer<RecordFormat> container = new BeanItemContainer<RecordFormat>(RecordFormat.class);

    Set<RecordFormat> selectedItemIds;

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            addComponent(buttonBar);

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
        }
        
        buttonBar.addButtonRight("Export", FontAwesome.DOWNLOAD, (e)->export());

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSizeFull();
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "ordinalSetting" });
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Ordinal" });
        table.setCellStyleGenerator(new TableCellStyleGenerator());
        table.setEditable(false);
        table.setMultiSelect(true);
        if (!readOnly) {
            table.setDragMode(TableDragMode.MULTIROW);
            table.setDropHandler(new TableDropHandler());
        }
        addComponent(table);
        setExpandRatio(table, 1.0f);

        Model model = component.getInputModel();

        if (model != null) {
            model = context.getConfigurationService().findModel(model.getId());
            List<RecordFormat> attributes = new ArrayList<RecordFormat>();

            for (ModelEntity entity : model.getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    attributes.add(new RecordFormat(entity, attr));
                }
            }

            Collections.sort(attributes, new Comparator<RecordFormat>() {
                public int compare(RecordFormat o1, RecordFormat o2) {
                    return new Integer(o1.getOrdinalSetting()).compareTo(new Integer(o2.getOrdinalSetting()));
                }
            });

            for (RecordFormat recordFormat : attributes) {
                table.addItem(recordFormat);
            }
        }
        calculatePositions();
        saveOrdinalSettings();
    }

    protected void export() {
        String fileNamePrefix = component.getName().toLowerCase().replace(' ', '-');
        ExportDialog dialog = new ExportDialog(table, fileNamePrefix, component.getName());
        UI.getCurrent().addWindow(dialog);
    }
    
    @SuppressWarnings("unchecked")
    protected Set<RecordFormat> getSelectedItems() {
        return (Set<RecordFormat>) table.getValue();
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
        for (RecordFormat record : container.getItemIds()) {
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
            table.refreshRowCache();
        }
    }

    protected void moveItemsTo(Set<RecordFormat> itemIds, int index) {
        if (index >= 0 && index < container.getItemIds().size() && itemIds.size() > 0) {
            int firstItemIndex = container.indexOfId(itemIds.iterator().next());
            if (index != firstItemIndex) {
                for (RecordFormat itemId : itemIds) {
                    boolean movingUp = index < container.indexOfId(itemId);
                    container.removeItem(itemId);
                    container.addItemAt(index, itemId);
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
        for (RecordFormat record : container.getItemIds()) {
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

    class MoveUpClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<RecordFormat> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                RecordFormat firstItem = itemIds.iterator().next();
                int index = container.indexOfId(firstItem) - 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveDownClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<RecordFormat> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                RecordFormat lastItem = null;
                Iterator<RecordFormat> iter = itemIds.iterator();
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
            Set<RecordFormat> itemIds = getSelectedItems();
            selectedItemIds = new LinkedHashSet<RecordFormat>(itemIds);
            for (RecordFormat itemId : itemIds) {
                table.unselect(itemId);
            }
            table.refreshRowCache();
        }
    }

    class PasteClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<RecordFormat> itemIds = getSelectedItems();
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
                RecordFormat target = (RecordFormat) targetDetails.getItemIdOver();
                moveItemsTo(getSelectedItems(), container.indexOfId(target));
            }
        }

        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }

    class TableCellStyleGenerator implements CellStyleGenerator {
        public String getStyle(Table source, Object itemId, Object propertyId) {
            if (propertyId != null && selectedItemIds != null && selectedItemIds.contains(itemId)) {
                return "highlight";
            }
            return null;
        }
    }

    public class RecordFormat {
        ModelEntity modelEntity;

        ModelAttrib modelAttribute;

        Map<Object, Field<?>> fields = new HashMap<Object, Field<?>>();

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

        public Field<?> getFocusField() {
            Field<?> field = fields.get(focusFieldId);
            if (field == null) {
                field = fields.get("width");
            }
            return field;
        }

        public int getOrdinalSetting() {
            return ordinalSetting;
        }

        public void setOrdinalSetting(int ordinalSetting) {
            this.ordinalSetting = ordinalSetting;
        }
    }
}
