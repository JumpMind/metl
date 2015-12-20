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
import java.util.List;

import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.DataDiff;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;

public class EditDataDiffPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;
    
    Table entityTable = new Table();
    BeanItemContainer<EntitySettings> entitySettingsContainer = new BeanItemContainer<EntitySettings>(EntitySettings.class);
    List<EntitySettings> entitySettings = new ArrayList<EntitySettings>();
    
    Table attributeTable = new Table();
    BeanItemContainer<AttributeSettings> attributeSettingsContainer = new BeanItemContainer<AttributeSettings>(AttributeSettings.class);
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    TextField entityFilterField;

    Button editButton;
    
    EditAttributesWindow attributeWindow;
    
    protected void buildUI() {

    	buildButtonBar();
    	buildEntityTable();
    	fillEntityContainer();
        updateEntityTable(null);
        buildAttributeWindow();
    }

    protected void buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
        editButton = buttonBar.addButton("Edit Columns", FontAwesome.EDIT);
        editButton.addClickListener(new EditButtonClickListener());
        editButton.setEnabled(false);
        entityFilterField = buttonBar.addFilter();
        entityFilterField.addTextChangeListener(event -> updateEntityTable(event.getText()));
        addComponent(buttonBar);    	
    }

    class EditButtonClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;
        public void buttonClick(ClickEvent event) {
        	refreshAttributeContainer((EntitySettings) entityTable.getValue());
        	updateAttributeTable();
            attributeWindow.show();
        }   
    }    
    
    protected void buildEntityTable() {
        entityTable.setContainerDataSource(entitySettingsContainer);
        entityTable.setSelectable(true);
        entityTable.setSortEnabled(false);
        entityTable.setImmediate(true);
        entityTable.setSortEnabled(false);
        entityTable.setSizeFull();
        entityTable.addGeneratedColumn("entityName", new ColumnGenerator() {
            private static final long serialVersionUID = 1L;
            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                EntitySettings setting = (EntitySettings) itemId;
                Model model = component.getInputModel();
                ModelEntity entity = model.getEntityById(setting.getEntityId());
                return UiUtils.getName(entityFilterField.getValue(), entity.getName());
            }
        });
        entityTable.setVisibleColumns(new Object[] { "entityName", "addEnabled", "updateEnabled", "deleteEnabled" });
        entityTable.setColumnWidth("entityName", 250);
        entityTable.setColumnHeaders(new String[] { "Entity Name", "Add Enabled", "Chg Enabled", "Del Enabled" });
        entityTable.setColumnExpandRatio("entityName", 1);
        entityTable.setTableFieldFactory(new EditEntityFieldFactory());
        entityTable.setEditable(true);
        entityTable.addItemClickListener(new ItemClickListener() {
            private static final long serialVersionUID = 1L;
        	public void itemClick(ItemClickEvent event) {
        		if (entityTable.getValue() == null) {
        			editButton.setEnabled(true);
        		} else {
        			editButton.setEnabled(false);
        		}
        	}
        });
        addComponent(entityTable);        
        setExpandRatio(entityTable, 1.0f);
    }
    
    protected void fillEntityContainer() {  	
        if (component.getInputModel() != null) {

            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                ComponentEntitySetting insert = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_ADD_ENABLED);
                ComponentEntitySetting update = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_CHG_ENABLED);
                ComponentEntitySetting delete = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_DEL_ENABLED);
                boolean insertEnabled = insert != null ? Boolean.parseBoolean(insert.getValue()) : true;
                boolean updateEnabled = update != null ? Boolean.parseBoolean(update.getValue()) : true;
                boolean deleteEnabled = delete != null ? Boolean.parseBoolean(delete.getValue()) : true;
                entitySettings.add(new EntitySettings(entity.getId(), insertEnabled, updateEnabled, deleteEnabled));
            }
        }    	
    }
    
    protected void updateEntityTable(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        entityFilterField.setValue(filter);
        entityTable.removeAllItems();
        for (EntitySettings entitySetting : entitySettings) {
            Model model = component.getInputModel();
            ModelEntity entity = model.getEntityById(entitySetting.getEntityId());
            if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)) {
            	entityTable.addItem(entitySetting);
            }
        }
    }
    
    class EditEntityFieldFactory implements TableFieldFactory {
        private static final long serialVersionUID = 1L;
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final EntitySettings settings = (EntitySettings) itemId;

            if (propertyId.equals("addEnabled")) {
                return createEntityCheckBox(settings, DataDiff.ENTITY_ADD_ENABLED);
            } else if (propertyId.equals("updateEnabled")) {
                return createEntityCheckBox(settings, DataDiff.ENTITY_CHG_ENABLED);
            } else if (propertyId.equals("deleteEnabled")) {
                return createEntityCheckBox(settings, DataDiff.ENTITY_DEL_ENABLED);
            } else {
                return null;
            }
        }
    }

    protected CheckBox createEntityCheckBox(final EntitySettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.setImmediate(true);
        checkBox.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;
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

    public static class EntitySettings implements Serializable {
        private static final long serialVersionUID = 1L;
        String entityId;
        boolean addEnabled;
        boolean updateEnabled;
        boolean deleteEnabled;

        public EntitySettings(String entityId, boolean insertEnabled, boolean updateEnabled, boolean deleteEnabled) {
            this.entityId = entityId;
            this.addEnabled = insertEnabled;
            this.updateEnabled = updateEnabled;
            this.deleteEnabled = deleteEnabled;
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

    //attribute window and support
    
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
			buildAttributeTable();
			addComponent(buildButtonFooter(buildCloseButton()));
		}
    	
        private void buildAttributeTable() {
     	   
     	   attributeTable.setContainerDataSource(attributeSettingsContainer);
     	   attributeTable.setSelectable(true);
     	   attributeTable.setSortEnabled(false);
     	   attributeTable.setImmediate(true);
     	   attributeTable.setSortEnabled(false);
     	   attributeTable.setSizeFull();
     	   attributeTable.addGeneratedColumn("attributeName", new ColumnGenerator() {
     	      private static final long serialVersionUID = 1L;
                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
             	   AttributeSettings setting = (AttributeSettings) itemId;
                    Model model = component.getInputModel();
                    ModelAttribute attribute = model.getAttributeById(setting.getAttributeId());
                    return UiUtils.getName(entityFilterField.getValue(), attribute.getName());
                }
            });
     	   attributeTable.setVisibleColumns(new Object[] { "attributeName", "compareEnabled" });
     	   attributeTable.setColumnWidth("attributeName", 250);
     	   attributeTable.setColumnHeaders(new String[] { "Attribute Name", "Compare Enabled" });
     	   attributeTable.setColumnExpandRatio("attributeName", 1);
     	   attributeTable.setTableFieldFactory(new EditAttributeFieldFactory());
     	   attributeTable.setEditable(true);
            addComponent(attributeTable, 1);
            
        }
    }
        
    private void refreshAttributeContainer(EntitySettings selectedRow) {
  	   attributeSettings.clear();
  	   ModelEntity entity = component.getInputModel().getEntityById(selectedRow.getEntityId());
  	   for (ModelAttribute attribute : entity.getModelAttributes()) {
  		   
            ComponentAttributeSetting compare = component.getSingleAttributeSetting(attribute.getId(), DataDiff.ATTRIBUTE_COMPARE_ENABLED);
            boolean compareEnabled = compare != null ? Boolean.parseBoolean(compare.getValue()) : true;
            attributeSettings.add(new AttributeSettings(attribute.getId(), compareEnabled, attribute.isPk() == true?true:false));     		        		   
  	   }
     }

    protected void updateAttributeTable() {
        attributeTable.removeAllItems();
        for (AttributeSettings attributeSetting : attributeSettings) {
            attributeTable.addItem(attributeSetting);
        }
    }    
    
    class EditAttributeFieldFactory implements TableFieldFactory {
        private static final long serialVersionUID = 1L;
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final AttributeSettings settings = (AttributeSettings) itemId;
            if (propertyId.equals("compareEnabled")) {
                return createAttributeCheckBox(settings, DataDiff.ATTRIBUTE_COMPARE_ENABLED);
            } else {
                return null;
            }
        }
    }
    
    protected CheckBox createAttributeCheckBox(final AttributeSettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.setImmediate(true);
        if (settings.isPrimaryKey()) {
        	checkBox.setEnabled(false);
        }
        checkBox.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void valueChange(ValueChangeEvent event) {
                ComponentAttributeSetting setting = component.getSingleAttributeSetting(settings.getAttributeId(), key);

                String oldValue = setting == null ? Boolean.TRUE.toString() : setting.getValue();
                if (setting == null) {
                    setting = new ComponentAttributeSetting(settings.getAttributeId(), component.getId(), key, Boolean.TRUE.toString());
                    component.addAttributeSetting(setting);
                }
                setting.setValue(checkBox.getValue().toString());
                if (!oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
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
    
}
