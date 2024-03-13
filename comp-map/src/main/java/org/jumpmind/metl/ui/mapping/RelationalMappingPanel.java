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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

@SuppressWarnings("serial")
public class RelationalMappingPanel extends AbstractFlowStepAwareComponentEditPanel implements IMappingPanel {
    
    VerticalLayout diagramLayout;

    MappingDiagram diagram;

    Button removeButton;

    Checkbox srcMapFilter;

    Checkbox dstMapFilter;

    TextField srcTextFilter;

    TextField dstTextFilter;    
    
    RelationalModel inputModel;
    
    RelationalModel outputModel;
    
    protected void buildUI() {
        
        inputModel = ((RelationalModel)component.getInputModel());
        outputModel = ((RelationalModel)component.getOutputModel());
        
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            add(buttonBar);
            Button autoMapButton = buttonBar.addButton("Auto Map", VaadinIcon.FLASH);
            removeButton = buttonBar.addButton("Remove", VaadinIcon.TRASH);
            removeButton.setEnabled(false);
            autoMapButton.addClickListener(new AutoMapListener());
            removeButton.addClickListener(new RemoveListener());
            Button removeAllLink = buttonBar.addButton("Remove All Links", VaadinIcon.CLOSE);
            removeAllLink.addClickListener(new RemoveAllListener());

        }
        buttonBar.addButtonRight("Export", VaadinIcon.DOWNLOAD, (e)->export());

        HorizontalLayout titleHeader = new HorizontalLayout();
        titleHeader.setSpacing(true);
        titleHeader.getStyle().set("margin", "0 16px");
        titleHeader.setWidthFull();
		Html inputModelHtml = new Html("<span><b>Input Model:</b> &nbsp;" + (inputModel != null ? inputModel.getName() : "?") + "</span>");
        Html outputModelHtml = new Html("<span><b>Output Model:</b> &nbsp;" + (outputModel != null ? outputModel.getName() : "?") + "</span>");
        titleHeader.add(inputModelHtml, outputModelHtml);
        add(titleHeader);

        HorizontalLayout filterHeader = new HorizontalLayout();
        filterHeader.setSpacing(true);
        filterHeader.setMargin(true);
        filterHeader.setWidthFull();
        HorizontalLayout srcFilterHeader = new HorizontalLayout();
        srcFilterHeader.setSpacing(true);
        srcFilterHeader.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        HorizontalLayout dstFilterHeader = new HorizontalLayout();
        dstFilterHeader.setSpacing(true);
        dstFilterHeader.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        filterHeader.add(srcFilterHeader, dstFilterHeader);
        add(filterHeader);

        srcTextFilter = new TextField();
        srcTextFilter.setWidth("20em");
        srcTextFilter.setPlaceholder("Filter");
        srcTextFilter.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        srcTextFilter.setValueChangeMode(ValueChangeMode.LAZY);
        srcTextFilter.setValueChangeTimeout(200);
        srcTextFilter.addValueChangeListener(new FilterInputTextListener());

        srcMapFilter = new Checkbox("Mapped Only");
        srcMapFilter.addValueChangeListener(new FilterSrcMapListener());
        srcFilterHeader.add(srcTextFilter, srcMapFilter);

        dstTextFilter = new TextField();
        dstTextFilter.setWidth("20em");
        dstTextFilter.setPlaceholder("Filter");
        dstTextFilter.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        dstTextFilter.setValueChangeMode(ValueChangeMode.LAZY);
        dstTextFilter.setValueChangeTimeout(200);
        dstTextFilter.addValueChangeListener(new FilterOutputTextListener());

        dstMapFilter = new Checkbox("Mapped Only");
        dstMapFilter.addValueChangeListener(new FilterDstMapListener());
        dstFilterHeader.add(dstTextFilter, dstMapFilter);

        diagramLayout = new VerticalLayout();
        diagramLayout.setWidthFull();
        diagramLayout.setHeight("10000px");
        redrawDiagram();
        Scroller scroller = new Scroller(diagramLayout);
        scroller.setSizeFull();
        add(scroller);
        expand(scroller);
        
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
        if (inputModel != null) {
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
    
    protected void redrawDiagram() {
        if (diagram != null) {
            diagramLayout.remove(diagram);
        }
        
        diagram = new MappingDiagram(context, component, this);
        diagram.setSizeFull();
        diagramLayout.add(diagram);
    }

    protected void export() {
        Grid<String[]> table = new Grid<String[]>();
        table.addColumn(item -> item[0]).setHeader("Source Entity");
        table.addColumn(item -> item[1]).setHeader("Source Attribute");
        table.addColumn(item -> item[2]).setHeader("Destination Entity");
        table.addColumn(item -> item[3]).setHeader("Destination Attribute");
        
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
    
    @Override
    public void selectEvent(SelectEvent event) {
        removeButton.setEnabled(event.getSelectedSourceId() != null);
    }

    class RemoveAllListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            new ConfirmDialog("Delete ALL Links?", "Are you sure you want to remove all of the connections between source and target attributes?", "Ok", e -> {
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
                }
                        
                removeButton.setEnabled(false); 
                redrawDiagram();
            }).open();
        }
    }

    class RemoveListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            diagram.removeSelected();
            removeButton.setEnabled(false);
            redrawDiagram();
        }
    }

    class AutoMapListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            autoMap(false);
            autoMap(true);
            redrawDiagram();
        }
    }
    
    class FilterInputTextListener implements ValueChangeListener<ValueChangeEvent<String>> {
        public void valueChanged(ValueChangeEvent<String> event) {
            diagram.filterInputModel(event.getValue(), srcMapFilter.getValue());
        }
    }

    class FilterOutputTextListener implements ValueChangeListener<ValueChangeEvent<String>> {
        public void valueChanged(ValueChangeEvent<String> event) {
            diagram.filterOutputModel(event.getValue(), dstMapFilter.getValue());
        }
    }

    class FilterSrcMapListener implements ValueChangeListener<ValueChangeEvent<Boolean>> {
        public void valueChanged(ValueChangeEvent<Boolean> event) {
            diagram.filterInputModel(srcTextFilter.getValue(), event.getValue());
        }
    }

    class FilterDstMapListener implements ValueChangeListener<ValueChangeEvent<Boolean>> {
        public void valueChanged(ValueChangeEvent<Boolean> event) {
            diagram.filterOutputModel(dstTextFilter.getValue(), event.getValue());
        }
    }        
}
