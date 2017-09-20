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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.Sorter;
import org.jumpmind.metl.ui.common.ButtonBar;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
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
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EditSorterPanel extends AbstractComponentEditPanel {

    Table table = new Table();

    BeanItemContainer<RecordFormat> container = new BeanItemContainer<RecordFormat>(RecordFormat.class);

    TextField filterTextField;

    Set<RecordFormat> selectedItemIds;
    
    static final String SHOW_ALL = "Show All Entities";
    static final String SHOW_POPULATED_ENTITIES = "Filter Populated Entites";

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button moveUpButton = buttonBar.addButton("Move Up", FontAwesome.ARROW_UP);
        moveUpButton.addClickListener(new MoveUpClickListener());

        Button moveDownButton = buttonBar.addButton("Move Down", FontAwesome.ARROW_DOWN);
        moveDownButton.addClickListener(new MoveDownClickListener());

        Button moveTopButton = buttonBar.addButton("Move Top", FontAwesome.ANGLE_DOUBLE_UP);
        moveTopButton.addClickListener(new MoveTopClickListener());

        Button moveBottomButton = buttonBar.addButton("Move Bottom", FontAwesome.ANGLE_DOUBLE_DOWN);
        moveBottomButton.addClickListener(new MoveBottomClickListener());
        
        filterTextField = buttonBar.addFilter();
        filterTextField.addTextChangeListener(event -> updateTable(event.getText()));

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSizeFull();
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "ordinalSetting", "sortSetting" });
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Sort Order", "Sort" });
        table.setTableFieldFactory(new EditFieldFactory());
        table.setCellStyleGenerator(new TableCellStyleGenerator());
        table.setEditable(true);
        table.setMultiSelect(true);
        addComponent(table);
        setExpandRatio(table, 1.0f);

        updateTable(null);
        
        calculatePositions();
        saveOrdinalSettings();
        saveSortSettings();
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
    
    protected void updateTable(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        filterTextField.setValue(filter);
        table.removeAllItems();
        Model model = component.getInputModel();

        if (model != null) {
            model = context.getConfigurationService().findModel(model.getId());
            List<RecordFormat> checkedAttributes = new ArrayList<RecordFormat>();
            List<RecordFormat> attributes = new ArrayList<RecordFormat>();

            for (ModelEntity entity : model.getModelEntities()) {
                if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)) {
	                for (ModelAttrib attr : entity.getModelAttributes()) {
	                    RecordFormat recFormat = new RecordFormat(entity, attr);
	                    if (recFormat.getSortSetting()) {
	                    	checkedAttributes.add(recFormat);
	                    } else {
	                    	attributes.add(recFormat);
	                    }
	                }
                }
            }

            Collections.sort(attributes, new Comparator<RecordFormat>() {
                public int compare(RecordFormat o1, RecordFormat o2) {
                	return new String(o1.getEntityName()).compareTo(new String(o2.getEntityName()));
                }
            });

            Collections.sort(checkedAttributes, new Comparator<RecordFormat>() {
                public int compare(RecordFormat o1, RecordFormat o2) {
                	int sortOrder = new String(o1.getEntityName()).compareTo(new String(o2.getEntityName()));

                	if (sortOrder != 0) {
                		return sortOrder;
                	} else {
                		return new Integer(o1.getOrdinalSetting()==0?999999:o1.getOrdinalSetting()).compareTo(new Integer(o2.getOrdinalSetting()==0?999999:o2.getOrdinalSetting()));
                	}
                }
            });

            for (RecordFormat recordFormat : checkedAttributes) {
                table.addItem(recordFormat);
            }

            for (RecordFormat recordFormat : attributes) {
                table.addItem(recordFormat);
            }
        }
    }

    protected void calculatePositions() {
        boolean needsRefreshed = false;
        int ordinal = 1;
        for (RecordFormat record : container.getItemIds()) {
        	if (record.getSortSetting()) {
	            if (record.getOrdinalSetting() != ordinal) {
	                record.setOrdinalSetting(ordinal);
	                needsRefreshed = true;
	            }
	            ordinal++;
        	} else {
	            if (record.getOrdinalSetting() != 0) {
	            	record.setOrdinalSetting(0);
	            	needsRefreshed = true;
	            }
        	}
        }
        
        if (needsRefreshed) {
            RecordFormat record = getSelectedItem();
            if (record != null) {
                record.setFocusFieldId("sortSetting");
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
        table.refreshRowCache();
        }
    }

    protected void saveOrdinalSettings() {
        String attrName = Sorter.SORTER_ATTRIBUTE_ORDINAL;
        int ordinal = 1;
        for (RecordFormat record : container.getItemIds()) {
        	if (record.getSortSetting()) {
        		saveSetting(record.getAttributeId(), attrName, String.valueOf(ordinal));
            	ordinal++;
        	} else {
        		saveSetting(record.getAttributeId(), attrName, "0");
        	}
        }
    }

    protected void saveSortSettings() {
        String attrName = Sorter.ATTRIBUTE_SORTER_ENABLED;

        for (RecordFormat record : container.getItemIds()) {
            saveSetting(record.getAttributeId(), attrName, String.valueOf(record.getSortSetting()));
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
    
    protected int getLastSortItemIndex(String entityId, String attributeId, boolean isSelected) {
    	int lastSortItemIndex = -1;
    	int lastEntitySortIndex = -1;
    	int lastItemIndex = -1;
    	int lastEntityIndex = -1;
    	for (RecordFormat record : container.getItemIds()) {
    		if (isSelected && record.getSortSetting() && !attributeId.equals(record.getAttributeId())) {
    			lastSortItemIndex++;
    			if (entityId.equals(record.getEntityId())) {
	    			lastEntitySortIndex = container.indexOfId(record);
    			}
    		}
			lastItemIndex++;
			if (!record.getSortSetting() && entityId.equals(record.getEntityId()) && !attributeId.equals(record.getAttributeId())) {
				lastEntityIndex = container.indexOfId(record);
			}
    	}
    	
    	if (isSelected) {
	    	if (lastEntitySortIndex < lastSortItemIndex && lastEntitySortIndex != -1) {    	
	    		return lastEntitySortIndex;
	    	} else {
	    		return lastSortItemIndex;
	    	}
    	} else {
	    	if (lastEntityIndex < lastItemIndex && lastEntityIndex != -1) {    	
	    		return lastEntityIndex;
	    	} else {
	    		return lastItemIndex;
	    	}
    	}
    }

    class MoveUpClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        	Set<RecordFormat> selectedSortRecords = new HashSet<RecordFormat>();
        	int previousEntityLastIndex = -1;
        	String selectedEntityId = "";
        	for (RecordFormat record : getSelectedItems()) {
        		if (record.getSortSetting()) {
        			selectedSortRecords.add(record);
        			if (selectedEntityId == "") {
        				selectedEntityId = record.getEntityId();
        			} else if (!selectedEntityId.equals(record.getEntityId())) {
        				// if an additional selected attribute is from a different entity then don't move anything
        				// can only move within one entity at a time
        				selectedSortRecords.clear();
        				break;
        			}
        		}
        	}
        	
            if (selectedSortRecords.size() > 0 && selectedSortRecords != null) {
            	// loop through list of selected records to see if moving an attribute into another entity
            	// add the remaining attributes to keep the entity as a whole together o/w just move the attribute within the entity
            	for (RecordFormat record : container.getItemIds()) {
            		if (record.getSortSetting()) {
            			if (!selectedEntityId.equals(record.getEntityId())) {
            				previousEntityLastIndex = container.indexOfId(record);
            			} else {
            				break;
            			}
            		}
            	}

                RecordFormat firstItem = selectedSortRecords.iterator().next();
                int index = container.indexOfId(firstItem) - 1;
                if (index >= 0 && index > previousEntityLastIndex) {
                	moveItemsTo(selectedSortRecords, index);
                }
            }
        }
    }

    class MoveDownClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        	Set<RecordFormat> selectedSortRecords = new HashSet<RecordFormat>();
        	int nextEntityFirstIndex = -1;
        	String selectedEntityId = "";
        	for (RecordFormat record : getSelectedItems()) {
        		if (record.getSortSetting()) {
        			selectedSortRecords.add(record);
        			if (selectedEntityId == "") {
        				selectedEntityId = record.getEntityId();
        			} else if (!selectedEntityId.equals(record.getEntityId())) {
        				// if an additional selected attribute is from a different entity then don't move anything
        				// can only move within one entity at a time
        				selectedSortRecords.clear();
        				break;
        			}
        		}
        	}
        	
        	if (selectedSortRecords.size() > 0 && selectedSortRecords != null) {
            	int lastSortItemIndex = 0;
            	for (RecordFormat record : container.getItemIds()) {
            		if (record.getSortSetting()) {
            			lastSortItemIndex++;
            		}
            	}

            	// loop through list of selected records to see if moving an attribute into another entity
            	// add the remaining attributes to keep the entity as a whole together o/w just move the attribute within the entity
            	ListIterator<RecordFormat> allItemsIterator = container.getItemIds().listIterator(container.getItemIds().size());
                while (allItemsIterator.hasPrevious()) {
                	RecordFormat record = allItemsIterator.previous();
            		if (record.getSortSetting()) {
            			if (!selectedEntityId.equals(record.getEntityId())) {
            				nextEntityFirstIndex = container.indexOfId(record);
            			} else {
            				break;
            			}
            		}
                }
        	
                RecordFormat lastSortItem = null;
                Iterator<RecordFormat> iter = selectedSortRecords.iterator();
                while (iter.hasNext()) {
                	lastSortItem = iter.next();
                }
                
                int index = container.indexOfId(lastSortItem) + 1;
                if (index < lastSortItemIndex && (index < nextEntityFirstIndex || nextEntityFirstIndex == -1)) {
                	moveItemsTo(selectedSortRecords, index);
                }
        	}
        }
    }

    class MoveTopClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        	Set<RecordFormat> selectedSortRecords = new HashSet<RecordFormat>();
        	int previousEntityLastIndex = -1;
        	String selectedEntityId = "";
        	for (RecordFormat record : getSelectedItems()) {
        		if (record.getSortSetting()) {
        			selectedSortRecords.add(record);
        			if (selectedEntityId == "") {
        				selectedEntityId = record.getEntityId();
        			} else if (!selectedEntityId.equals(record.getEntityId())) {
        				// if an additional selected attribute is from a different entity then don't move anything
        				// can only move within one entity at a time
        				selectedSortRecords.clear();
        				break;
        			}
        		}
        	}
        	
        	if (selectedSortRecords.size() > 0 && selectedSortRecords != null) {
	        	// loop through list of selected records to see if moving an attribute into another entity
	        	// add the remaining attributes to keep the entity as a whole together o/w just move the attribute within the entity
	        	for (RecordFormat record : container.getItemIds()) {
	        		if (record.getSortSetting()) {
	        			if (!selectedEntityId.equals(record.getEntityId())) {
	        				previousEntityLastIndex = container.indexOfId(record);
	        			} else {
	        				break;
	        			}
	        		}
	        	}
	        	
	        	if (previousEntityLastIndex == -1) {
	        		moveItemsTo(selectedSortRecords, 0);
	        	} else {
	        		moveItemsTo(selectedSortRecords, previousEntityLastIndex + 1);
	        	}
        	}
        }
    }

    class MoveBottomClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        	Set<RecordFormat> selectedSortRecords = new HashSet<RecordFormat>();
        	int nextEntityFirstIndex = -1;
        	String selectedEntityId = "";
        	for (RecordFormat record : getSelectedItems()) {
        		if (record.getSortSetting()) {
        			selectedSortRecords.add(record);
        			if (selectedEntityId == "") {
        				selectedEntityId = record.getEntityId();
        			} else if (!selectedEntityId.equals(record.getEntityId())) {
        				// if an additional selected attribute is from a different entity then don't move anything
        				// can only move within one entity at a time
        				selectedSortRecords.clear();
        				break;
        			}
        		}
        	}

        	if (selectedSortRecords.size() > 0 && selectedSortRecords != null) {
	        	// loop through list of selected records to see if moving an attribute into another entity
	        	// add the remaining attributes to keep the entity as a whole together o/w just move the attribute within the entity
	        	ListIterator<RecordFormat> allItemsIterator = container.getItemIds().listIterator(container.getItemIds().size());
	            while (allItemsIterator.hasPrevious()) {
	            	RecordFormat record = allItemsIterator.previous();
	        		if (record.getSortSetting()) {
	        			if (!selectedEntityId.equals(record.getEntityId())) {
	        				nextEntityFirstIndex = container.indexOfId(record);
	        			} else {
	        				break;
	        			}
	        		}
	            }
	            
	        	RecordFormat lastSortItem = null;
	        	for (RecordFormat record : container.getItemIds()) {
	        		if (record.getSortSetting()) {
	        			lastSortItem = record;
	        		}
	        	}
	        	
	        	if (nextEntityFirstIndex == -1) {
		            int index = container.indexOfId(lastSortItem);
	        		moveItemsTo(selectedSortRecords, index);
	        	} else {
		        	moveItemsTo(selectedSortRecords, nextEntityFirstIndex - 1);
	        	}
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

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final RecordFormat record = (RecordFormat) itemId;
            Field<?> field = null;
            if (propertyId.equals("sortSetting")) {
                return createAttributeCheckBox(record, Sorter.ATTRIBUTE_SORTER_ENABLED);
            }

            RecordFormat selected = getSelectedItem();
            if (selected == itemId && record.getFocusField() != null) {
                record.getFocusField().focus();
            }
            return field;
        }

        protected CheckBox createAttributeCheckBox(final RecordFormat record, final String key) {
            final CheckBox checkBox = new CheckBox();
            checkBox.setImmediate(true);
            checkBox.addValueChangeListener(new ValueChangeListener() {
                private static final long serialVersionUID = 1L;
                @Override
                public void valueChange(ValueChangeEvent event) {
                    ComponentAttribSetting setting = component.getSingleAttributeSetting(record.getAttributeId(), key);

                    String oldValue = setting == null ? Boolean.FALSE.toString() : setting.getValue();
                    if (setting == null) {
                        setting = new ComponentAttribSetting(record.getAttributeId(), component.getId(), key, Boolean.FALSE.toString());
                        component.addAttributeSetting(setting);
                    }
                    setting.setValue(checkBox.getValue().toString());
                    if (!oldValue.equals(setting.getValue())) {
                        context.getConfigurationService().save(setting);   
                        Set<RecordFormat> attributes = new HashSet<>();
                        attributes.add(record);
                        
                        if (oldValue.equals(Boolean.FALSE.toString())) { 
	                        moveItemsTo(attributes, getLastSortItemIndex(record.getEntityId(), record.getAttributeId(), true) + 1);
                        } else {
                        	moveItemsTo(attributes, getLastSortItemIndex(record.getEntityId(), record.getAttributeId(), false));
                        }
                    }

                    calculatePositions();
                    saveOrdinalSettings();
                    saveSortSettings();
                }
            });
            return checkBox;
        }
        
        protected void focusOn(RecordFormat record, Object propertyId) {
            record.setFocusFieldId(propertyId);
            for (Object itemId : getSelectedItems()) {
                table.unselect(itemId);
            }
            table.select(record);
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

        boolean sortSetting = false;

        int ordinalSetting;
        
        public RecordFormat(ModelEntity modelEntity, ModelAttrib modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;
            
            if (modelAttribute != null) {
            	ComponentAttribSetting setting = component.getSingleAttributeSetting(modelAttribute.getId(),
            		Sorter.SORTER_ATTRIBUTE_ORDINAL);
	            if (setting != null) {
	                this.ordinalSetting = Integer.parseInt(setting.getValue());
	            }

	            setting = component.getSingleAttributeSetting(modelAttribute.getId(),
	                    Sorter.ATTRIBUTE_SORTER_ENABLED);
	            if (setting != null) {
	                this.sortSetting = Boolean.parseBoolean(setting.getValue());
	            }
            } else {
            	ComponentEntitySetting setting = component.getSingleEntitySetting(modelEntity.getId(),
                		Sorter.SORTER_ATTRIBUTE_ORDINAL);
	            if (setting != null) {
	                this.ordinalSetting = Integer.parseInt(setting.getValue());
	            }

	            setting = component.getSingleEntitySetting(modelEntity.getId(),
	                    Sorter.ATTRIBUTE_SORTER_ENABLED);
	            if (setting != null) {
	                this.sortSetting = Boolean.parseBoolean(setting.getValue());
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

        public String getEntityId() {
            return modelEntity.getId();
        }

        public String getAttributeName() {
            return modelAttribute.getName();
        }

        public String getAttributeId() {
            return modelAttribute.getId();
        }

        public boolean getSortSetting() {
            return sortSetting;
        }

        public void setSortSetting(boolean sortSetting) {
            this.sortSetting = sortSetting;
        }

        public void addField(Object id, Field<?> field) {
            fields.put(id, field);
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
