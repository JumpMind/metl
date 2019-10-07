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
package org.jumpmind.metl.ui.mapping;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashMap;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentModelSetting;
import org.jumpmind.metl.core.model.ComponentModelSetting.Type;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.runtime.component.Mapping;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.TableV7DataProvider;
import org.jumpmind.metl.ui.views.design.AbstractFlowStepAwareComponentEditPanel;
import org.jumpmind.vaadin.ui.common.ExportDialog;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.event.FieldEvents.TextChangeEvent;
import com.vaadin.v7.event.FieldEvents.TextChangeListener;
import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;

@SuppressWarnings("serial")
public class RelationalMappingPanel extends AbstractFlowStepAwareComponentEditPanel {

    MappingDiagram diagram;

    Button removeButton;

    CheckBox srcMapFilter;

    CheckBox dstMapFilter;

    TextField srcTextFilter;

    TextField dstTextFilter;    
    
    RelationalModel inputModel;
    
    RelationalModel outputModel;
    
    protected void buildUI() {
        
        inputModel = ((RelationalModel)component.getInputModel());
        outputModel = ((RelationalModel)component.getOutputModel());
        
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            addComponent(buttonBar);
            Button autoMapButton = buttonBar.addButton("Auto Map", FontAwesome.FLASH);
            removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
            removeButton.setEnabled(false);
            autoMapButton.addClickListener(new AutoMapListener());
            removeButton.addClickListener(new RemoveListener());
        }
        buttonBar.addButtonRight("Export", FontAwesome.DOWNLOAD, (e)->export());

        HorizontalLayout titleHeader = new HorizontalLayout();
        titleHeader.setSpacing(true);
        titleHeader.setMargin(new MarginInfo(false, true, false, true));
        titleHeader.setWidth(100f, Unit.PERCENTAGE);
        titleHeader.addComponent(
                new Label("<b>Input Model:</b> &nbsp;" + (inputModel != null ? inputModel.getName() : "?"),
                        ContentMode.HTML));
        titleHeader.addComponent(
                new Label("<b>Output Model:</b> &nbsp;" + (outputModel != null ? outputModel.getName() : "?"),
                        ContentMode.HTML));
        addComponent(titleHeader);

        HorizontalLayout filterHeader = new HorizontalLayout();
        filterHeader.setSpacing(true);
        filterHeader.setMargin(new MarginInfo(true, true, true, true));
        filterHeader.setWidth(100f, Unit.PERCENTAGE);
        HorizontalLayout srcFilterHeader = new HorizontalLayout();
        srcFilterHeader.setSpacing(true);
        srcFilterHeader.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        filterHeader.addComponent(srcFilterHeader);
        HorizontalLayout dstFilterHeader = new HorizontalLayout();
        dstFilterHeader.setSpacing(true);
        dstFilterHeader.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        filterHeader.addComponent(dstFilterHeader);
        addComponent(filterHeader);

        srcTextFilter = new TextField();
        srcTextFilter.setWidth(20, Unit.EM);
        srcTextFilter.setInputPrompt("Filter");
        srcTextFilter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        srcTextFilter.setIcon(FontAwesome.SEARCH);
        srcTextFilter.setImmediate(true);
        srcTextFilter.setTextChangeEventMode(TextChangeEventMode.LAZY);
        srcTextFilter.setTextChangeTimeout(200);
        srcTextFilter.addTextChangeListener(new FilterInputTextListener());
        srcFilterHeader.addComponent(srcTextFilter);

        srcMapFilter = new CheckBox("Mapped Only");
        srcMapFilter.addValueChangeListener(new FilterSrcMapListener());
        srcFilterHeader.addComponent(srcMapFilter);

        dstTextFilter = new TextField();
        dstTextFilter.setWidth(20, Unit.EM);
        dstTextFilter.setInputPrompt("Filter");
        dstTextFilter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        dstTextFilter.setIcon(FontAwesome.SEARCH);
        dstTextFilter.setImmediate(true);
        dstTextFilter.setTextChangeEventMode(TextChangeEventMode.LAZY);
        dstTextFilter.setTextChangeTimeout(200);
        dstTextFilter.addTextChangeListener(new FilterOutputTextListener());
        dstFilterHeader.addComponent(dstTextFilter);

        dstMapFilter = new CheckBox("Mapped Only");
        dstMapFilter.addValueChangeListener(new FilterDstMapListener());
        dstFilterHeader.addComponent(dstMapFilter);

        Panel panel = new Panel();
        VerticalLayout vlay = new VerticalLayout();
        vlay.setSizeFull();
        diagram = new MappingDiagram(context, component, readOnly);
        diagram.setSizeFull();
        vlay.addComponent(diagram);
        panel.setContent(vlay);
        panel.setSizeFull();
        addComponent(panel);
        setExpandRatio(panel, 1.0f);
        diagram.addListener(new EventListener());
        
    }    
    
    @Override
    public void selected() {
        if (isNotBlank(srcTextFilter.getValue()) || srcMapFilter.getValue()) {
            diagram.filterInputModel(srcTextFilter.getValue().trim(), srcMapFilter.getValue());
        }
        
        if (isNotBlank(dstTextFilter.getValue()) || dstMapFilter.getValue()) {
            diagram.filterOutputModel(dstTextFilter.getValue().trim(), dstMapFilter.getValue());
        }
    }

    protected void autoMap(boolean fuzzy) {
    	HashMap<ModelEntity, String> inputEntityMap = new HashMap<ModelEntity, String>();
    	HashMap<ModelEntity, String> outputEntityMap = new HashMap<ModelEntity, String>();
    	
    	// check if there are filters, then only auto-map the filtered entities
    	if (isNotBlank(srcTextFilter.getValue()) && srcMapFilter.getValue()) {
            for (ModelEntity ent : inputModel.getModelEntities()) {
            	if (isNotBlank(srcTextFilter.getValue()) && ent.getName().toUpperCase().indexOf(srcTextFilter.getValue().toUpperCase()) != -1) {
            		for (ModelAttrib attr : ent.getModelAttributes()) {
            			boolean isMapped = false;
            	    	for (ComponentModelSetting setting : component.getModelSettings()) {
            	            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getModelObjectId().equals(attr.getId())) {
            	            	inputEntityMap.put(ent,"found");
            	            	isMapped = true;
            	            	break;
            	            }
            	        }
            	    	if (isMapped) {
            	    		break;
            	    	}
            		}
            	} else {
            		for (ModelAttrib attr : ent.getModelAttributes()) {
            			if (isNotBlank(srcTextFilter.getValue()) && attr.getName().toUpperCase().indexOf(srcTextFilter.getValue().toUpperCase()) != -1) {
	            			boolean isMapped = false;
	            	    	for (ComponentModelSetting setting : component.getModelSettings()) {
	            	            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getModelObjectId().equals(attr.getId())) {
	            	            	inputEntityMap.put(ent,"found");
	            	                isMapped = true;
	            	            	break;
	            	            }
	            	        }
	            	    	if (isMapped) {
	            	    		break;
	            	    	}
            			}
            		}
            	}
            }
    	} else if (isNotBlank(srcTextFilter.getValue()) || srcMapFilter.getValue()) {
            for (ModelEntity ent : inputModel.getModelEntities()) {
            	if (isNotBlank(srcTextFilter.getValue()) && ent.getName().toUpperCase().indexOf(srcTextFilter.getValue().toUpperCase()) != -1) {
            		inputEntityMap.put(ent,"found");
            		continue;
            	} else {
            		for (ModelAttrib attr : ent.getModelAttributes()) {
            			if (isNotBlank(srcTextFilter.getValue()) && attr.getName().toUpperCase().indexOf(srcTextFilter.getValue().toUpperCase()) != -1) {
            				inputEntityMap.put(ent,"found");
                    		break;
            			}
            			if (srcMapFilter.getValue()) {
	            			boolean isMapped = false;
	            	    	for (ComponentModelSetting setting : component.getModelSettings()) {
	            	            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getModelObjectId().equals(attr.getId())) {
	            	            	inputEntityMap.put(ent,"found");
	            	                isMapped = true;
	            	            	break;
	            	            }
	            	        }
	            	    	if (isMapped) {
	            	    		break;
	            	    	}
            			}
            		}
            	}
            }
    	}
    	if (isNotBlank(dstTextFilter.getValue()) && dstMapFilter.getValue()) {
            for (ModelEntity ent : outputModel.getModelEntities()) {
            	if (isNotBlank(dstTextFilter.getValue()) && ent.getName().toUpperCase().indexOf(dstTextFilter.getValue().toUpperCase()) != -1) {
            		for (ModelAttrib attr : ent.getModelAttributes()) {
            			boolean isMapped = false;
            	    	for (ComponentModelSetting setting : component.getModelSettings()) {
            	            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getValue().equals(attr.getId())) {
            	            	outputEntityMap.put(ent,"found");
            	            	isMapped = true;
            	            	break;
            	            }
            	        }
            	    	if (isMapped) {
            	    		break;
            	    	}
            		}
            	} else {
            		for (ModelAttrib attr : ent.getModelAttributes()) {
            			if (isNotBlank(dstTextFilter.getValue()) && attr.getName().toUpperCase().indexOf(dstTextFilter.getValue().toUpperCase()) != -1) {
	            			boolean isMapped = false;
	            	    	for (ComponentModelSetting setting : component.getModelSettings()) {
	            	            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getValue().equals(attr.getId())) {
	            	            	outputEntityMap.put(ent,"found");
	            	                isMapped = true;
	            	            	break;
	            	            }
	            	        }
	            	    	if (isMapped) {
	            	    		break;
	            	    	}
            			}
            		}
            	}
            }
    	} else if (isNotBlank(dstTextFilter.getValue()) || dstMapFilter.getValue()) {
            for (ModelEntity ent : outputModel.getModelEntities()) {
            	if (isNotBlank(dstTextFilter.getValue()) && ent.getName().toUpperCase().indexOf(dstTextFilter.getValue().toUpperCase()) != -1) {
            		outputEntityMap.put(ent,"found");
            		continue;
            	} else {
            		for (ModelAttrib attr : ent.getModelAttributes()) {
            			if (isNotBlank(dstTextFilter.getValue()) && attr.getName().toUpperCase().indexOf(dstTextFilter.getValue().toUpperCase()) != -1) {
            				outputEntityMap.put(ent,"found");
                    		break;
            			}
            			if (dstMapFilter.getValue()) {
	            			boolean isMapped = false;
	            	    	for (ComponentModelSetting setting : component.getModelSettings()) {
	            	            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getValue().equals(attr.getId())) {
	            	            	outputEntityMap.put(ent,"found");
	            	                isMapped = true;
	            	            	break;
	            	            }
	            	        }
	            	    	if (isMapped) {
	            	    		break;
	            	    	}
            			}
            		}
            	}
            }
    	}
        for (ModelEntity entity1 : inputModel.getModelEntities()) {
        	if ((srcTextFilter.getValue().isEmpty() && !srcMapFilter.getValue()) || inputEntityMap.containsKey(entity1)) {
	            for (ModelAttrib attr : entity1.getModelAttributes()) {
	                /* look for exact match first */
	                for (ModelEntity entity2 : outputModel.getModelEntities()) {
	                	if ((dstTextFilter.getValue().isEmpty() && !dstMapFilter.getValue()) || outputEntityMap.containsKey(entity2)) {
		                    boolean foundExactMatch = false;
		                    for (ModelAttrib attr2 : entity2.getModelAttributes()) {
		                    	foundExactMatch = autoMap(entity1, entity2, attr, attr2, fuzzy, true);
		                    	if (foundExactMatch) {
		                    		break;
		                    	}
		                    }
		
		                    if (!foundExactMatch && fuzzy) {
		                        for (ModelAttrib attr2 : entity2.getModelAttributes()) {
		                            if (autoMap(entity1, entity2, attr, attr2, fuzzy, false)) {
		                            	break;
		                            }
		                        }
		                    }
	                	}
	                }
	            }
        	}
        }
    }

    protected boolean autoMap(ModelEntity entity1, ModelEntity entity2, ModelAttrib attr, ModelAttrib attr2, boolean fuzzy,
            boolean exact) {
        boolean isMapped = false;
        boolean isFound = false;
        boolean exactMatch = exact && attr.getName().equalsIgnoreCase(attr2.getName()) && entity1.getName().equals(entity2.getName());
        for (ComponentModelSetting setting : component.getModelSettings()) {
            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getValue().equals(attr2.getId())) {
            	isFound = true;
                break;
            }
        }
        if (!isFound && ((fuzzy && fuzzyMatches(attr.getName(), attr2.getName()))
                || ((!exact && attr.getName().equalsIgnoreCase(attr2.getName())) || exactMatch))) {
            ComponentModelSetting setting = new ComponentModelSetting();
            setting.setComponentId(component.getId());
            setting.setName(Mapping.MODEL_OBJECT_MAPS_TO);
            setting.setValue(attr2.getId());
            setting.setModelObjectId(attr.getId());
            setting.setType(Type.ATTRIBUTE.toString());
            component.addModelSetting(setting);
            context.getConfigurationService().save(setting);
            diagram.markAsDirty();
            isMapped = true;
        }

        return isMapped;
    }

    protected boolean fuzzyMatches(String str1, String str2) {
        int x = computeLevenshteinDistance(str1, str2);
        return x < 3;
    }

    protected int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 1; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
            }
        }

        return distance[str1.length()][str2.length()];
    }

    protected int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    protected void export() {
        Table table = new Table();
        table.addContainerProperty("Source Entity", String.class, null);
        table.addContainerProperty("Source Attribute",  String.class, null);
        table.addContainerProperty("Destination Entity", String.class, null);
        table.addContainerProperty("Destination Attribute",  String.class, null);
        
        int itemId = 0;
        for (ComponentAttribSetting setting : component.getAttributeSettings()) {
            if (Mapping.ATTRIBUTE_MAPS_TO.equals(setting.getName())) {
                ModelAttrib srcAttribute = inputModel.getAttributeById(setting.getAttributeId());
                ModelEntity srcEntity = inputModel.getEntityById(srcAttribute.getEntityId());
                ModelAttrib dstAttribute = outputModel.getAttributeById(setting.getValue());
                ModelEntity dstEntity = outputModel.getEntityById(dstAttribute.getEntityId());
                
                table.addItem(new Object[]{srcEntity.getName(), srcAttribute.getName(), dstEntity.getName(), dstAttribute.getName()}, itemId++);
            }
        }
        
        String fileNamePrefix = component.getName().toLowerCase().replace(' ', '-');
        ExportDialog dialog = new ExportDialog(new TableV7DataProvider(table), fileNamePrefix, component.getName());
        UI.getCurrent().addWindow(dialog);
    }

    class EventListener implements Listener {
        public void componentEvent(Event event) {
            if (event instanceof SelectEvent) {
                SelectEvent selectEvent = (SelectEvent) event;
                removeButton.setEnabled(selectEvent.getSelectedSourceId() != null);
            }
        }
    }

    class RemoveListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            diagram.removeSelected();
            removeButton.setEnabled(false);
        }
    }

    class AutoMapListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            autoMap(false);
            autoMap(true);
        }
    }
    
    class FilterInputTextListener implements TextChangeListener {
        public void textChange(TextChangeEvent event) {
            diagram.filterInputModel((String) event.getText(), srcMapFilter.getValue());
        }
    }

    class FilterOutputTextListener implements TextChangeListener {
        public void textChange(TextChangeEvent event) {
            diagram.filterOutputModel((String) event.getText(), dstMapFilter.getValue());
        }
    }

    class FilterSrcMapListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            diagram.filterInputModel(srcTextFilter.getValue(), (boolean) event.getProperty().getValue());
        }
    }

    class FilterDstMapListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            diagram.filterOutputModel(dstTextFilter.getValue(), (boolean) event.getProperty().getValue());
        }
    }        
}
