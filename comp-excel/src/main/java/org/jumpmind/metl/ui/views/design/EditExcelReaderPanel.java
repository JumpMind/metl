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

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.ExcelFileReader;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.IsNull;
import com.vaadin.data.util.filter.Not;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

public class EditExcelReaderPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;

    Grid grid;

    BeanItemContainer<Record> container;    
    
    @Override
    protected void buildUI() {
        
        buildGrid();
        refresh();
    }
    
    protected void buildGrid() {
        grid = new Grid();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.setSizeFull();
        grid.setEditorEnabled(!readOnly);
        container = new BeanItemContainer<Record>(Record.class);
        grid.setContainerDataSource(container);
        grid.setColumnOrder("entityName", "attributeName", "excelMapping");
        HeaderRow filterRow = grid.appendHeaderRow();
        addColumn("entityName", filterRow);
        addColumn("attributeName", filterRow);
        TextField tfExcelMapping = new TextField();
        tfExcelMapping.addBlurListener(e->saveExcelMappingSettings());
        tfExcelMapping.setWidth(100, Unit.PERCENTAGE);
        tfExcelMapping.setImmediate(true);
        grid.getColumn("excelMapping").setEditorField(tfExcelMapping).setExpandRatio(1);
        addShowPopulatedFilter("excelMapping", filterRow);
        grid.setEditorBuffered(false);
        addComponent(grid);
        setExpandRatio(grid, 1);
    }

    protected void addColumn(String propertyId, HeaderRow filterRow) {
        grid.getColumn(propertyId).setEditable(false);
        HeaderCell cell = filterRow.getCell(propertyId);
        TextField filterField = new TextField();
        filterField.setInputPrompt("Filter");
        filterField.setImmediate(true);
        filterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        filterField.setWidth(100, Unit.PERCENTAGE);
        filterField.addTextChangeListener(change -> {
            container.removeContainerFilters(propertyId);
            if (!change.getText().isEmpty()) {
                container.addContainerFilter(
                        new SimpleStringFilter(propertyId, change.getText(), true, false));
            }
        });
        cell.setComponent(filterField);
    }    
    
    protected void addShowPopulatedFilter(String propertyId, HeaderRow filterRow) {
        HeaderCell cell = filterRow.getCell(propertyId);
        CheckBox group = new CheckBox("Show Set Only");
        group.setImmediate(true);
        group.addValueChangeListener(l->{
            container.removeContainerFilters(propertyId);
            if (group.getValue()) {
                container.addContainerFilter(new And(new Not(new Compare.Equal(propertyId,"")), new Not(new IsNull(propertyId))));
            }
        });
        group.addStyleName(ValoTheme.CHECKBOX_SMALL);
        cell.setComponent(group);
        
    }

    protected void saveExcelMappingSettings() {
        for (Object obj : container.getItemIds()) {
            Record record = (Record) obj;
            if (record.modelAttribute != null) {
                saveAttributeSetting(record.modelAttribute.getId(),
                        ExcelFileReader.SETTING_EXCEL_MAPPING,
                        StringUtils.trimToNull(record.getExcelMapping()));
            }
        }
    }

    protected void saveAttributeSetting(String attributeId, String name, String value) {
        ComponentAttribSetting setting = component.getSingleAttributeSetting(attributeId, name);
        if (setting == null && value != null) {
            setting = new ComponentAttribSetting(attributeId, name, value);
            setting.setComponentId(component.getId());
            component.addAttributeSetting(setting);
            context.getConfigurationService().save(setting);
        } else if (setting != null && !StringUtils.equals(setting.getValue(), value)) {
            if (value == null) {
                setting.setValue(value);
                context.getConfigurationService().delete(setting);
            } else {
                setting.setValue(value);
                context.getConfigurationService().save(setting);
            }
        }
    }
    
    public class Record implements Serializable {

        private static final long serialVersionUID = 1L;

        ModelEntity modelEntity;

        ModelAttrib modelAttribute;

        String excelMapping = "";

        public Record(ModelEntity modelEntity, ModelAttrib modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;
            if (modelAttribute != null) {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(
                        modelAttribute.getId(), ExcelFileReader.SETTING_EXCEL_MAPPING);
                if (setting != null) {
                    excelMapping = setting.getValue();
                }
            }
        }

        public int hashCode() {
            return modelEntity.hashCode()
                    + (modelAttribute == null ? 0 : modelAttribute.hashCode());
        }

        public boolean equals(Object obj) {
            if (obj instanceof Record) {
                return hashCode() == ((Record) obj).hashCode();
            }
            return super.equals(obj);
        }

        public String getEntityName() {
            return modelEntity.getName();
        }

        public String getAttributeName() {
            if (modelAttribute != null) {
                return modelAttribute.getName();
            }
            return null;
        }

        public String getExcelMapping() {
            return excelMapping;
        }

        public void setExcelMapping(String excelMapping) {
            this.excelMapping = excelMapping;
        }
    }
    
    protected void refresh() {
        Model model = component.getOutputModel();
        if (model != null) {
            Collections.sort(model.getModelEntities(), new Comparator<ModelEntity>() {
                public int compare(ModelEntity entity1, ModelEntity entity2) {
                    return entity1.getName().toLowerCase()
                            .compareTo(entity2.getName().toLowerCase());
                }
            });

            for (ModelEntity entity : model.getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    container.addItem(new Record(entity, attr));
                }
            }
        }
    }
}
