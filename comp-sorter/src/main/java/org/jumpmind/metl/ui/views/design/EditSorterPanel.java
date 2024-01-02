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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.Sorter;
import org.jumpmind.metl.ui.common.ButtonBar;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EditSorterPanel extends AbstractComponentEditPanel {
    
    List<RecordFormat> recordFormatList = new ArrayList<RecordFormat>();

    Grid<RecordFormat> grid = new Grid<RecordFormat>();

    TextField filterTextField;

    Set<RecordFormat> selectedItemIds;
    
    static final String SHOW_ALL = "Show All Entities";
    static final String SHOW_POPULATED_ENTITIES = "Filter Populated Entites";

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button moveUpButton = buttonBar.addButton("Move Up", VaadinIcons.ARROW_UP);
        moveUpButton.addClickListener(new MoveUpClickListener());

        Button moveDownButton = buttonBar.addButton("Move Down", VaadinIcons.ARROW_DOWN);
        moveDownButton.addClickListener(new MoveDownClickListener());

        Button moveTopButton = buttonBar.addButton("Move Top", VaadinIcons.ANGLE_DOUBLE_UP);
        moveTopButton.addClickListener(new MoveTopClickListener());

        Button moveBottomButton = buttonBar.addButton("Move Bottom", VaadinIcons.ANGLE_DOUBLE_DOWN);
        moveBottomButton.addClickListener(new MoveBottomClickListener());
        
        filterTextField = buttonBar.addFilter();
        filterTextField.addValueChangeListener(event -> updateGrid(event.getValue()));

        grid.setSizeFull();
        grid.addColumn(RecordFormat::getEntityName).setCaption("Entity Name").setSortable(false);
        grid.addColumn(RecordFormat::getAttributeName).setCaption("Attribute Name").setSortable(false);
        grid.addColumn(RecordFormat::getOrdinalSetting).setCaption("Sort Order").setSortable(false);
        grid.addComponentColumn(setting -> createAttributeCheckBox(setting, Sorter.ATTRIBUTE_SORTER_ENABLED))
                .setCaption("Sort").setSortable(false);
        grid.setSelectionMode(SelectionMode.MULTI);
        addComponent(grid);
        setExpandRatio(grid, 1.0f);

        updateGrid(null);
        
        calculatePositions();
        saveOrdinalSettings();
        saveSortSettings();
    }

    protected Set<RecordFormat> getSelectedItems() {
        return grid.getSelectedItems();
    }
    
    protected void updateGrid(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        filterTextField.setValue(filter);
        RelationalModel model = (RelationalModel) component.getInputModel();
        recordFormatList = new ArrayList<RecordFormat>();

        if (model != null) {
            model = context.getConfigurationService().findRelationalModel(model.getId());
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
                recordFormatList.add(recordFormat);
            }

            for (RecordFormat recordFormat : attributes) {
                recordFormatList.add(recordFormat);
            }
        }
        grid.setItems(recordFormatList);
    }

    protected void calculatePositions() {
        boolean needsRefreshed = false;
        int ordinal = 1;
        for (RecordFormat record : recordFormatList) {
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
            grid.setItems(recordFormatList);
        }
    }

    protected void saveOrdinalSettings() {
        String attrName = Sorter.SORTER_ATTRIBUTE_ORDINAL;
        int ordinal = 1;
        for (RecordFormat record : recordFormatList) {
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

        for (RecordFormat record : recordFormatList) {
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
    	for (RecordFormat record : recordFormatList) {
    		if (isSelected && record.getSortSetting() && !attributeId.equals(record.getAttributeId())) {
    			lastSortItemIndex++;
    			if (entityId.equals(record.getEntityId())) {
	    			lastEntitySortIndex = recordFormatList.indexOf(record);
    			}
    		}
			lastItemIndex++;
			if (!record.getSortSetting() && entityId.equals(record.getEntityId()) && !attributeId.equals(record.getAttributeId())) {
				lastEntityIndex = recordFormatList.indexOf(record);
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
            	for (RecordFormat record : recordFormatList) {
            		if (record.getSortSetting()) {
            			if (!selectedEntityId.equals(record.getEntityId())) {
            				previousEntityLastIndex = recordFormatList.indexOf(record);
            			} else {
            				break;
            			}
            		}
            	}

                RecordFormat firstItem = selectedSortRecords.iterator().next();
                int index = recordFormatList.indexOf(firstItem) - 1;
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
            	for (RecordFormat record : recordFormatList) {
            		if (record.getSortSetting()) {
            			lastSortItemIndex++;
            		}
            	}

            	// loop through list of selected records to see if moving an attribute into another entity
            	// add the remaining attributes to keep the entity as a whole together o/w just move the attribute within the entity
            	ListIterator<RecordFormat> allItemsIterator = recordFormatList.listIterator(recordFormatList.size());
                while (allItemsIterator.hasPrevious()) {
                	RecordFormat record = allItemsIterator.previous();
            		if (record.getSortSetting()) {
            			if (!selectedEntityId.equals(record.getEntityId())) {
            				nextEntityFirstIndex = recordFormatList.indexOf(record);
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
                
                int index = recordFormatList.indexOf(lastSortItem) + 1;
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
	        	for (RecordFormat record : recordFormatList) {
	        		if (record.getSortSetting()) {
	        			if (!selectedEntityId.equals(record.getEntityId())) {
	        				previousEntityLastIndex = recordFormatList.indexOf(record);
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
	        	ListIterator<RecordFormat> allItemsIterator = recordFormatList.listIterator(recordFormatList.size());
	            while (allItemsIterator.hasPrevious()) {
	            	RecordFormat record = allItemsIterator.previous();
	        		if (record.getSortSetting()) {
	        			if (!selectedEntityId.equals(record.getEntityId())) {
	        				nextEntityFirstIndex = recordFormatList.indexOf(record);
	        			} else {
	        				break;
	        			}
	        		}
	            }
	            
	        	RecordFormat lastSortItem = null;
	        	for (RecordFormat record : recordFormatList) {
	        		if (record.getSortSetting()) {
	        			lastSortItem = record;
	        		}
	        	}
	        	
	        	if (nextEntityFirstIndex == -1) {
		            int index = recordFormatList.indexOf(lastSortItem);
	        		moveItemsTo(selectedSortRecords, index);
	        	} else {
		        	moveItemsTo(selectedSortRecords, nextEntityFirstIndex - 1);
	        	}
        	}
        }
    }

    protected CheckBox createAttributeCheckBox(final RecordFormat record, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.addValueChangeListener(new ValueChangeListener<Boolean>() {
            private static final long serialVersionUID = 1L;
            @Override
            public void valueChange(ValueChangeEvent<Boolean> event) {
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

    public class RecordFormat {
        ModelEntity modelEntity;

        ModelAttrib modelAttribute;

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

        public int getOrdinalSetting() {
            return ordinalSetting;
        }

        public void setOrdinalSetting(int ordinalSetting) {
            this.ordinalSetting = ordinalSetting;
        }
    }
}
