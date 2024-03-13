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
import java.util.List;

import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.RdbmsWriter;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class EditRdbmsWriterPanel extends AbstractComponentEditPanel {

    Grid<AttributeSettings> grid = new Grid<AttributeSettings>();

    TextField filterField;

    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    protected void buildUI() {
        setPadding(false);
        setSpacing(false);
        
        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);

        filterField = buttonBar.addFilter();
        filterField.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {

            @Override
            public void valueChanged(ValueChangeEvent<String> event) {
                filterField.setValue(event.getValue());
                updateGrid(event.getValue());
            }
        });

        grid.setSizeFull();
        grid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
            ModelEntity entity = model.getEntityById(attribute.getEntityId());
            return UiUtils.getName(filterField.getValue(), entity.getName());
        }).setHeader("Entity Name").setFlexGrow(0).setWidth("250px").setSortable(true);
        grid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
            return UiUtils.getName(filterField.getValue(), attribute.getName());
        }).setHeader("Attribute Name").setFlexGrow(0).setWidth("250px").setSortable(true);
        grid.addComponentColumn(setting -> createCheckbox(setting, RdbmsWriter.ATTRIBUTE_INSERT_ENABLED))
                .setHeader("Insert Enabled").setSortable(true);
        grid.addComponentColumn(setting -> createCheckbox(setting, RdbmsWriter.ATTRIBUTE_UPDATE_ENABLED))
                .setHeader("Update Enabled").setSortable(true);
        add(grid);
        expand(grid);

        if (component.getInputModel() != null) {

            for (ModelEntity entity : ((RelationalModel)component.getInputModel()).getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    ComponentAttribSetting insert = component.getSingleAttributeSetting(attr.getId(), RdbmsWriter.ATTRIBUTE_INSERT_ENABLED);
                    ComponentAttribSetting update = component.getSingleAttributeSetting(attr.getId(), RdbmsWriter.ATTRIBUTE_UPDATE_ENABLED);
                    boolean insertEnabled = insert != null ? Boolean.parseBoolean(insert.getValue()) : true;
                    boolean updateEnabled = update != null ? Boolean.parseBoolean(update.getValue()) : true;
                    attributeSettings.add(new AttributeSettings(attr.getId(), insertEnabled, updateEnabled));
                }
            }

            Collections.sort(attributeSettings, new Comparator<AttributeSettings>() {
                @Override
                public int compare(AttributeSettings o1, AttributeSettings o2) {
                    RelationalModel model = (RelationalModel) component.getInputModel();
                    ModelAttrib attribute1 = model.getAttributeById(o1.getAttributeId());
                    ModelEntity entity1 = model.getEntityById(attribute1.getEntityId());

                    ModelAttrib attribute2 = model.getAttributeById(o2.getAttributeId());
                    ModelEntity entity2 = model.getEntityById(attribute2.getEntityId());

                    int compare = entity1.getName().compareTo(entity2.getName());
                    if (compare == 0) {
                        compare = attribute1.getName().compareTo(attribute2.getName());
                    }
                    return compare;
                }
            });
        }

        updateGrid(null);

    }

    protected void updateGrid(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        List<AttributeSettings> filteredAttributeSettings = new ArrayList<AttributeSettings>();
        for (AttributeSettings attributeSetting : attributeSettings) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(attributeSetting.getAttributeId());
            ModelEntity entity = model.getEntityById(attribute.getEntityId());
            if (isBlank(filter) || entity.getName().toLowerCase().contains(filter) || attribute.getName().toLowerCase().contains(filter)) {
                filteredAttributeSettings.add(attributeSetting);
            }
        }
        grid.setItems(filteredAttributeSettings);
    }

    private Checkbox createCheckbox(final AttributeSettings settings, final String key) {
        final Checkbox checkbox = new Checkbox();
        if (!readOnly) {
            checkbox.addValueChangeListener((event) -> {
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
            });
        }
        checkbox.setReadOnly(readOnly);
        return checkbox;
    }

    public static class AttributeSettings {

        String attributeId;
        boolean insertEnabled;
        boolean updateEnabled;

        public AttributeSettings(String attributeId, boolean insertEnabled, boolean updateEnabled) {
            this.attributeId = attributeId;
            this.insertEnabled = insertEnabled;
            this.updateEnabled = updateEnabled;
        }

        public void setInsertEnabled(boolean insertEnabled) {
            this.insertEnabled = insertEnabled;
        }

        public boolean isInsertEnabled() {
            return insertEnabled;
        }

        public void setUpdateEnabled(boolean updateEnabled) {
            this.updateEnabled = updateEnabled;
        }

        public boolean isUpdateEnabled() {
            return updateEnabled;
        }

        public String getAttributeId() {
            return attributeId;
        }

        public void setAttributeId(String attributeId) {
            this.attributeId = attributeId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AttributeSettings) {
                return attributeId.equals(((AttributeSettings) obj).getAttributeId());
            } else {
                return super.equals(obj);
            }
        }

        @Override
        public int hashCode() {
            return attributeId.hashCode();
        }

    }

}
