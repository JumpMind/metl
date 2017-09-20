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
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.Merger;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EditMergerPanel extends AbstractComponentEditPanel {

    Table table = new Table();

    TextField filterField;

    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    BeanItemContainer<AttributeSettings> container = new BeanItemContainer<AttributeSettings>(AttributeSettings.class);

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(new TextChangeListener() {

            @Override
            public void textChange(TextChangeEvent event) {
                filterField.setValue(event.getText());
                updateTable(event.getText());
            }
        });

        addComponent(buttonBar);

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSortEnabled(true);
        table.setSizeFull();
        table.addGeneratedColumn("entityName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                AttributeSettings setting = (AttributeSettings) itemId;
                Model model = component.getInputModel();
                ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                return UiUtils.getName(filterField.getValue(), entity.getName());
            }
        });
        table.addGeneratedColumn("attributeName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                AttributeSettings setting = (AttributeSettings) itemId;
                Model model = component.getInputModel();
                ModelAttrib attribute = model.getAttributeById(setting.getAttributeId());
                return UiUtils.getName(filterField.getValue(), attribute.getName());
            }
        });
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "joinOn" });
        table.setColumnWidth("entityName", 250);
        table.setColumnWidth("attributeName", 250);
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Join On" });
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        addComponent(table);
        setExpandRatio(table, 1.0f);

        if (component.getInputModel() != null) {

            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    ComponentAttribSetting joinOn = component.getSingleAttributeSetting(attr.getId(), Merger.MERGE_ATTRIBUTE);

                    boolean join = joinOn != null ? Boolean.parseBoolean(joinOn.getValue()) : false;
                    attributeSettings.add(new AttributeSettings(attr.getId(), join));
                }
            }

            Collections.sort(attributeSettings, new Comparator<AttributeSettings>() {
                @Override
                public int compare(AttributeSettings o1, AttributeSettings o2) {
                    Model model = component.getInputModel();
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

        updateTable(null);

    }

    protected void updateTable(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        table.removeAllItems();
        for (AttributeSettings attributeSetting : attributeSettings) {
            Model model = component.getInputModel();
            ModelAttrib attribute = model.getAttributeById(attributeSetting.getAttributeId());
            ModelEntity entity = model.getEntityById(attribute.getEntityId());
            if (isBlank(filter) || entity.getName().toLowerCase().contains(filter) || attribute.getName().toLowerCase().contains(filter)) {
                table.addItem(attributeSetting);
            }
        }
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final AttributeSettings settings = (AttributeSettings) itemId;

            if (propertyId.equals("joinOn")) {
                return createCheckBox(settings, Merger.MERGE_ATTRIBUTE);
            } else {
                return null;
            }
        }
    }

    private CheckBox createCheckBox(final AttributeSettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.setImmediate(true);
        checkBox.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(settings.getAttributeId(), key);

                String oldValue = setting == null ? Boolean.FALSE.toString() : setting.getValue();
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
