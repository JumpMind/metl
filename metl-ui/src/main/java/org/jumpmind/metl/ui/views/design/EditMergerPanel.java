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
import org.jumpmind.metl.core.runtime.component.Merger;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class EditMergerPanel extends AbstractComponentEditPanel {

    Grid<AttributeSettings> grid = new Grid<AttributeSettings>();

    TextField filterField;

    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    protected void buildUI() {
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

        add(buttonBar);

        grid.setSizeFull();
        grid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
            ModelEntity entity = model.getEntityById(attribute.getEntityId());
            return UiUtils.getName(filterField.getValue(), entity.getName());
        }).setHeader("Entity Name").setWidth("250px").setSortable(true);
        grid.addColumn(setting -> {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
            return UiUtils.getName(filterField.getValue(), attribute.getName());
        }).setHeader("Attribute Name").setWidth("250px").setSortable(true);
        grid.addComponentColumn(setting -> createCheckbox(setting, Merger.MERGE_ATTRIBUTE)).setHeader("Join On").setSortable(true);
        add(grid);
        expand(grid);

        if (component.getInputModel() != null) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            for (ModelEntity entity : model.getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    ComponentAttribSetting joinOn = component.getSingleAttributeSetting(attr.getId(), Merger.MERGE_ATTRIBUTE);

                    boolean join = joinOn != null ? Boolean.parseBoolean(joinOn.getValue()) : false;
                    attributeSettings.add(new AttributeSettings(attr.getId(), join));
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
        checkbox.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<Boolean>>() {

            @Override
            public void valueChanged(ValueChangeEvent<Boolean> event) {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(settings.getAttributeId(), key);

                String oldValue = setting == null ? Boolean.FALSE.toString() : setting.getValue();
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

    public static class AttributeSettings {

        String attributeId;
        boolean joinOn;

        public AttributeSettings(String attributeId, boolean joinOn) {
            this.attributeId = attributeId;
            this.joinOn = joinOn;
        }

        public void setJoinOn(boolean joinOn) {
            this.joinOn = joinOn;
        }

        public boolean isJoinOn() {
            return joinOn;
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
