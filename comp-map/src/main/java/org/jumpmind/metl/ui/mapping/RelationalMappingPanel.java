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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentModelSetting;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ComponentModelSetting.Type;
import org.jumpmind.metl.core.runtime.component.Mapping;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;
import org.jumpmind.metl.ui.views.design.AbstractFlowStepAwareComponentEditPanel;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

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
            Button autoMapButton = buttonBar.addButton("Auto Map", VaadinIcons.FLASH);
            removeButton = buttonBar.addButton("Remove", VaadinIcons.TRASH);
            removeButton.setEnabled(false);
            autoMapButton.addClickListener(new AutoMapListener());
            removeButton.addClickListener(new RemoveListener());
            Button removeAllLink = buttonBar.addButton("Remove All Links", VaadinIcons.CLOSE);
            removeAllLink.addClickListener(new RemoveAllListener());

        }
        buttonBar.addButtonRight("Export", VaadinIcons.DOWNLOAD, (e)->export());

        HorizontalLayout titleHeader = new HorizontalLayout();
        titleHeader.setSpacing(true);
        titleHeader.setMargin(new MarginInfo(false, true, false, true));
        titleHeader.setWidth(100f, Unit.PERCENTAGE);
		Label inputModelLabel = new Label("<b>Input Model:</b> &nbsp;" + (inputModel != null ? inputModel.getName() : "?"));
		inputModelLabel.setContentMode(ContentMode.HTML);
        titleHeader.addComponent(inputModelLabel);
        Label outputModelLabel = new Label("<b>Output Model:</b> &nbsp;" + (outputModel != null ? outputModel.getName() : "?"));
        outputModelLabel.setContentMode(ContentMode.HTML);
        titleHeader.addComponent(outputModelLabel);
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
        srcTextFilter.setPlaceholder("Filter");
        srcTextFilter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        srcTextFilter.setIcon(VaadinIcons.SEARCH);
        srcTextFilter.setValueChangeMode(ValueChangeMode.LAZY);
        srcTextFilter.setValueChangeTimeout(200);
        srcTextFilter.addValueChangeListener(new FilterInputTextListener());
        srcFilterHeader.addComponent(srcTextFilter);

        srcMapFilter = new CheckBox("Mapped Only");
        srcMapFilter.addValueChangeListener(new FilterSrcMapListener());
        srcFilterHeader.addComponent(srcMapFilter);

        dstTextFilter = new TextField();
        dstTextFilter.setWidth(20, Unit.EM);
        dstTextFilter.setPlaceholder("Filter");
        dstTextFilter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        dstTextFilter.setIcon(VaadinIcons.SEARCH);
        dstTextFilter.setValueChangeMode(ValueChangeMode.LAZY);
        dstTextFilter.setValueChangeTimeout(200);
        dstTextFilter.addValueChangeListener(new FilterOutputTextListener());
        dstFilterHeader.addComponent(dstTextFilter);

        dstMapFilter = new CheckBox("Mapped Only");
        dstMapFilter.addValueChangeListener(new FilterDstMapListener());
        dstFilterHeader.addComponent(dstMapFilter);

        Panel panel = new Panel();
        VerticalLayout vlay = new VerticalLayout();
        vlay.setSizeFull();
        diagram = new MappingDiagram(context, component);
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
        for (ModelEntity entity1 : inputModel.getModelEntities()) {
            for (ModelAttrib attr : entity1.getModelAttributes()) {
                /* look for exact match first */
                for (ModelEntity entity2 : outputModel.getModelEntities()) {
                    boolean foundExactMatch = false;
                    for (ModelAttrib attr2 : entity2.getModelAttributes()) {
                        foundExactMatch |= autoMap(entity1, entity2, attr, attr2, fuzzy, true);
                    }

                    if (!foundExactMatch) {
                        for (ModelAttrib attr2 : entity2.getModelAttributes()) {
                            autoMap(entity1, entity2, attr, attr2, fuzzy, false);
                        }
                    }
                }
            }
        }
    }

    protected boolean autoMap(ModelEntity entity1, ModelEntity entity2, ModelAttrib attr, ModelAttrib attr2, boolean fuzzy,
            boolean exact) {
        boolean isMapped = false;
        boolean exactMatch = exact && attr.getName().equalsIgnoreCase(attr2.getName()) && entity1.getName().equals(entity2.getName());
        for (ComponentModelSetting setting : component.getModelSettings()) {
            if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO) && setting.getValue().equals(attr2.getId())) {
                isMapped = true;
                break;
            }
        }
        if (!isMapped && ((fuzzy && fuzzyMatches(attr.getName(), attr2.getName()))
                || ((!exact && attr.getName().equalsIgnoreCase(attr2.getName())) || exactMatch))) {
            ComponentModelSetting setting = new ComponentModelSetting();
            setting.setModelObjectId(attr.getId());
            setting.setType(Type.ATTRIBUTE.toString());
            setting.setComponentId(component.getId());
            setting.setName(Mapping.MODEL_OBJECT_MAPS_TO);
            setting.setValue(attr2.getId());
            component.addModelSetting(setting);
            context.getConfigurationService().save(setting);
            diagram.markAsDirty();
        }

        return exact;
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
        Grid<String[]> table = new Grid<String[]>();
        table.addColumn(item -> item[0]).setCaption("Source Entity");
        table.addColumn(item -> item[1]).setCaption("Source Attribute");
        table.addColumn(item -> item[2]).setCaption("Destination Entity");
        table.addColumn(item -> item[3]).setCaption("Destination Attribute");
        
        List<String[]> itemList = new ArrayList<String[]>();
        for (ComponentAttribSetting setting : component.getAttributeSettings()) {
            if (Mapping.MODEL_OBJECT_MAPS_TO.equals(setting.getName())) {
                ModelAttrib srcAttribute = inputModel.getAttributeById(setting.getAttributeId());
                ModelEntity srcEntity = inputModel.getEntityById(srcAttribute.getEntityId());
                ModelAttrib dstAttribute = outputModel.getAttributeById(setting.getValue());
                ModelEntity dstEntity = outputModel.getEntityById(dstAttribute.getEntityId());
                
                itemList.add(new String[]{srcEntity.getName(), srcAttribute.getName(), dstEntity.getName(), dstAttribute.getName()});
            }
        }
        table.setItems(itemList);
        
        ExportDialog.show(context, table);
    }
    
    class EventListener implements Listener {
        public void componentEvent(Event event) {
            if (event instanceof SelectEvent) {
                SelectEvent selectEvent = (SelectEvent) event;
                removeButton.setEnabled(selectEvent.getSelectedSourceId() != null);
            }
        }
    }

    class RemoveAllListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            ConfirmDialog.show("Delete ALL Links?", "Are you sure you want to remove all of the connections between source and target attributes?",
                    ()->{
	                    	Map<String, List<String>> linksMap = new HashMap<>();
	                    	for (ComponentModelSetting setting : component.getModelSettings()) {
	                    		if (setting.getName().equals(Mapping.MODEL_OBJECT_MAPS_TO)) {
	                        		List<String> linksList = new ArrayList<String>();
	                        		linksList.add(setting.getModelObjectId());
	                        		linksList.add(setting.getValue());
	                    			linksMap.put(setting.getModelObjectId() + "-" + setting.getValue(), linksList);
	                    		}
	                    	}
	                        for (String key : linksMap.keySet()) {
	                        	List<String> links = linksMap.get(key);
	                    		diagram.removeConnection(links.get(0), links.get(1));
	                        	diagram.markAsDirty();
	                        }
	                    	    	
	                    	removeButton.setEnabled(false);        
		            		return true;                    	
			            });
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
    
    class FilterInputTextListener implements ValueChangeListener<String> {
        public void valueChange(ValueChangeEvent<String> event) {
            diagram.filterInputModel(event.getValue(), srcMapFilter.getValue());
        }
    }

    class FilterOutputTextListener implements ValueChangeListener<String> {
        public void valueChange(ValueChangeEvent<String> event) {
            diagram.filterOutputModel(event.getValue(), dstMapFilter.getValue());
        }
    }

    class FilterSrcMapListener implements ValueChangeListener<Boolean> {
        public void valueChange(ValueChangeEvent<Boolean> event) {
            diagram.filterInputModel(srcTextFilter.getValue(), event.getValue());
        }
    }

    class FilterDstMapListener implements ValueChangeListener<Boolean> {
        public void valueChange(ValueChangeEvent<Boolean> event) {
            diagram.filterOutputModel(dstTextFilter.getValue(), event.getValue());
        }
    }        
}
