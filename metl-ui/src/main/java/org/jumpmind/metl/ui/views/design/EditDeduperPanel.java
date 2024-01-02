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
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.Deduper;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.grid.GridRowDragger;

public class EditDeduperPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;
    
    Grid<EntitySettings> entityGrid = new Grid<EntitySettings>();
    List<EntitySettings> entitySettings = new ArrayList<EntitySettings>();
    
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
        editButton = buttonBar.addButton("Edit Columns", VaadinIcons.EDIT);
        editButton.addClickListener(new EditButtonClickListener());
        entityFilterField = buttonBar.addFilter();
        entityFilterField.addValueChangeListener(event -> updateEntityGrid(event.getValue()));
        
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
        entityGrid.setSelectionMode(SelectionMode.MULTI);
        new GridRowDragger<EntitySettings>(entityGrid);
        addComponent(entityGrid);        
        setExpandRatio(entityGrid, 1.0f);
    }    

    protected void fillEntityContainer() {  	
        if (component.getInputModel() != null) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            for (ModelEntity entity : model.getModelEntities()) {
                entitySettings.add(new EntitySettings(entity.getId()));
            }
        }    	
    }
    
    protected void updateEntityGrid(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        entityFilterField.setValue(filter);
        List<EntitySettings> filteredEntitySettings = new ArrayList<EntitySettings>();
        for (EntitySettings entitySetting : entitySettings) {
            RelationalModel model = (RelationalModel) component.getInputModel();
            ModelEntity entity = model.getEntityById(entitySetting.getEntityId());
            if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)) {
            	filteredEntitySettings.add(entitySetting);
            }
        }
        entityGrid.setItems(filteredEntitySettings);
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

    public static class EntitySettings implements Serializable {
        private static final long serialVersionUID = 1L;
        String entityId;

        public EntitySettings(String entityId) {
            this.entityId = entityId;
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
			super("Edit Columns to Dedupe on");
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
           attributeGrid.addComponentColumn(setting -> createAttributeCheckBox(setting, Deduper.ATTRIBUTE_DEDUPE_ENABLED))
                   .setCaption("Dedupe Enabled").setSortable(false);
           addComponent(attributeGrid, 1);
            
        }
    }
        
    private void refreshAttributeContainer(EntitySettings selectedRow) {
  	   attributeSettings.clear();
  	   RelationalModel model = (RelationalModel) component.getInputModel();
  	   ModelEntity entity = model.getEntityById(selectedRow.getEntityId());
  	   for (ModelAttrib attribute : entity.getModelAttributes()) {
  		   
            ComponentAttribSetting compare = component.getSingleAttributeSetting(attribute.getId(), Deduper.ATTRIBUTE_DEDUPE_ENABLED);
            boolean dedupeEnabled = compare != null ? Boolean.parseBoolean(compare.getValue()) : false;
            attributeSettings.add(new AttributeSettings(attribute.getId(), dedupeEnabled, attribute.isPk() == true?true:false));     		        		   
  	   }
     }

    protected void updateAttributeGrid() {
        attributeGrid.setItems(attributeSettings);
    }    
    
    protected CheckBox createAttributeCheckBox(final AttributeSettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.addValueChangeListener(new ValueChangeListener<Boolean>() {
            private static final long serialVersionUID = 1L;
            @Override
            public void valueChange(ValueChangeEvent<Boolean> event) {
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
    
    public static class AttributeSettings implements Serializable {
        private static final long serialVersionUID = 1L;
        boolean primaryKey;
        String attributeId;
        boolean dedupeEnabled;

        public AttributeSettings(String attributeId, boolean dedupeEnabled, boolean primaryKey) {
            this.attributeId = attributeId;
            this.dedupeEnabled = dedupeEnabled;
            this.primaryKey = primaryKey;
        }

        public void setDedupeEnabled(boolean dedupeEnabled) {
            this.dedupeEnabled = dedupeEnabled;
        }

        public boolean isDedupeEnabled() {
            return dedupeEnabled;
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
