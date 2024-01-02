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
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.DelimitedFormatter;
import org.jumpmind.metl.core.runtime.component.DelimitedParser;
import org.jumpmind.metl.core.runtime.component.FixedLengthFormatter;
import org.jumpmind.metl.core.runtime.component.FixedLengthParser;
import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;

import com.vaadin.data.converter.StringToLongConverter;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.grid.GridRowDragger;

@SuppressWarnings("serial")
public class EditFormatPanel extends AbstractComponentEditPanel {
    
    List<RecordFormat> recordFormatList = new ArrayList<RecordFormat>();

    Grid<RecordFormat> grid = new Grid<RecordFormat>();

    Set<RecordFormat> selectedItemIds;

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            addComponent(buttonBar);

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
        
        buttonBar.addButtonRight("Export", VaadinIcons.DOWNLOAD, (e)->export());

        RelationalModel model = (RelationalModel) component.getInputModel();
        if (component.getType().equals(DelimitedParser.TYPE) || component.getType().equals(FixedLengthParser.TYPE)) {
            model = (RelationalModel) component.getOutputModel();
        }

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
                    return new Integer(o1.getOrdinalSetting()).compareTo(new Integer(o2.getOrdinalSetting()));
                }
            });

            recordFormatList.addAll(attributes);
        }

        grid.setSizeFull();
        boolean isFixedLength = component.getType().equals(FixedLengthFormatter.TYPE) || component.getType().equals(FixedLengthParser.TYPE);
        grid.addColumn(RecordFormat::getEntityName).setCaption("Entity Name");
        grid.addColumn(RecordFormat::getAttributeName).setCaption("Attribute Name");
        if (isFixedLength) {
            if (readOnly) {
                grid.addColumn(RecordFormat::getWidth).setCaption("Width");
            } else {
                final TextField textField = new TextField();
                textField.setWidth(100, Unit.PERCENTAGE);
                grid.addColumn(RecordFormat::getWidth)
                        .setEditorBinding(grid.getEditor().getBinder().forField(textField)
                                .withConverter(new StringToLongConverter("Width must be an integer"))
                                .bind(RecordFormat::getWidth, RecordFormat::setWidth))
                        .setCaption("Width");
            }
            
            grid.addColumn(RecordFormat::getStartPos).setCaption("Start Position");
            grid.addColumn(RecordFormat::getEndPos).setCaption("End Position");
        } else {
            grid.addColumn(RecordFormat::getOrdinalSetting).setCaption("Ordinal");
        }
        if (readOnly) {
            grid.addColumn(RecordFormat::getTransformText).setCaption("Transform");
        } else {
            final ComboBox<String> combo = new ComboBox<String>();
            combo.setWidth(100, Unit.PERCENTAGE);
            List<String> itemList = new ArrayList<String>();
            String[] functions = ModelAttributeScriptHelper.getSignatures();
            for (String function : functions) {
                itemList.add(function);
            }
            combo.setPageLength(functions.length > 20 ? 20 : functions.length);
            for (RecordFormat record : recordFormatList) {
                if (record.getTransformText() != null && !itemList.contains(record.getTransformText())) {
                    itemList.add(record.getTransformText());
                }
            }
            combo.setItems(itemList);
            combo.setNewItemProvider(newItem -> {
                itemList.add(newItem);
                combo.setItems(itemList);
                combo.setValue(newItem);
                return Optional.of(newItem);
            });
            grid.addColumn(RecordFormat::getTransformText).setEditorComponent(combo, RecordFormat::setTransformText).setCaption("Transform");
            grid.getEditor().setEnabled(true).addSaveListener(event -> {
                if (isFixedLength) {
                    calculatePositions();
                    saveLengthSettings();
                }
                saveTransformSettings();
            });
        }
        for (Column<RecordFormat, ?> column : grid.getColumns()) {
            column.setSortable(false);
            if (isFixedLength) {
                column.setWidth(75);
            }
        }
        grid.setSelectionMode(SelectionMode.MULTI);
        if (!readOnly) {
            new GridRowDragger<RecordFormat>(grid);
        }
        addComponent(grid);
        setExpandRatio(grid, 1.0f);

        grid.setItems(recordFormatList);
        calculatePositions();
        saveOrdinalSettings();
        saveLengthSettings();
        saveTransformSettings();
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
        if (component.getType().equals(FixedLengthFormatter.TYPE) || component.getType().equals(FixedLengthParser.TYPE)) {
            long pos = 1;
            for (RecordFormat record : recordFormatList) {
                if (record.getStartPos() != pos) {
                    record.setStartPos(pos);
                    needsRefreshed = true;
                }
                long endPos = pos + record.getWidth() - 1;
                if (record.getEndPos() != endPos) {
                    record.setEndPos(endPos);
                    needsRefreshed = true;
                }
                pos = endPos + 1;
            }

        } else if (component.getType().equals(DelimitedFormatter.TYPE) || component.getType().equals(DelimitedParser.TYPE)) {
            int ordinal = 1;
            for (RecordFormat record : recordFormatList) {
                if (record.getOrdinalSetting() != ordinal) {
                    record.setOrdinalSetting(ordinal);
                    needsRefreshed = true;
                }
                ordinal++;
            }
        }
        if (needsRefreshed) {
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
        if (component.getType().equals(FixedLengthFormatter.TYPE) || component.getType().equals(FixedLengthParser.TYPE)) {
            attrName = FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_ORDINAL;
        } else {
            attrName = DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL;
        }
        int ordinal = 1;
        for (RecordFormat record : recordFormatList) {
            saveSetting(record.getAttributeId(), attrName, String.valueOf(ordinal));
            ordinal++;
        }
    }

    protected void saveLengthSettings() {
        if (component.getType().equals(FixedLengthFormatter.TYPE) || component.getType().equals(FixedLengthParser.TYPE)) {
            for (RecordFormat record : recordFormatList) {
                saveSetting(record.getAttributeId(), FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH,
                        String.valueOf(record.getWidth()));
            }
        }
    }

    protected void saveTransformSettings() {
        String attrName;
        if (component.getType().equals(FixedLengthFormatter.TYPE) || component.getType().equals(FixedLengthParser.TYPE)) {
            attrName = FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION;
        } else {
            attrName = DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION;
        }

        for (RecordFormat record : recordFormatList) {
            saveSetting(record.getAttributeId(), attrName, record.getTransformText());
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
                int index = recordFormatList.indexOf(firstItem) - 1;
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
                int index = recordFormatList.indexOf(lastItem) + 1;
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
            moveItemsTo(getSelectedItems(), recordFormatList.size() - 1);
        }
    }

    class CutClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<RecordFormat> itemIds = getSelectedItems();
            selectedItemIds = new LinkedHashSet<RecordFormat>(itemIds);
            grid.deselectAll();
        }
    }

    class PasteClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
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

        long width = 1;

        long startPos;

        long endPos;

        String transformText = "";

        int ordinalSetting;

        public RecordFormat(ModelEntity modelEntity, ModelAttrib modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;

            if (component.getType().equals(DelimitedFormatter.TYPE) || component.getType().equals(DelimitedParser.TYPE)) {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                        DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL);
                if (setting != null) {
                    this.ordinalSetting = Integer.parseInt(setting.getValue());
                }

                setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                        DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION);
                if (setting != null) {
                    this.transformText = setting.getValue();
                }

            } else {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                        FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH);
                if (setting != null) {
                    this.width = Long.parseLong(setting.getValue());
                }

                setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                        FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_ORDINAL);
                if (setting != null) {
                    this.ordinalSetting = Integer.parseInt(setting.getValue());
                }

                setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                        FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION);
                if (setting != null) {
                    this.transformText = setting.getValue();
                }

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

        public long getWidth() {
            return width;
        }

        public void setWidth(long width) {
            this.width = width;
        }

        public String getTransformText() {
            return transformText;
        }

        public void setTransformText(String transformText) {
            this.transformText = transformText;
        }

        public long getStartPos() {
            return startPos;
        }

        public void setStartPos(long startPos) {
            this.startPos = startPos;
        }

        public long getEndPos() {
            return endPos;
        }

        public void setEndPos(long endPos) {
            this.endPos = endPos;
        }

        public int getOrdinalSetting() {
            return ordinalSetting;
        }

        public void setOrdinalSetting(int ordinalSetting) {
            this.ordinalSetting = ordinalSetting;
        }
    }
}
