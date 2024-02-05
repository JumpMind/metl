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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.HierarchicalModel;
import org.jumpmind.metl.core.model.ModelSchemaObject;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.runtime.component.Mapping;
import org.jumpmind.metl.core.runtime.component.RelationalHierarchicalMapping;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;
import org.jumpmind.metl.ui.views.design.AbstractFlowStepAwareComponentEditPanel;
import org.jumpmind.vaadin.ui.common.ResizableDialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

@SuppressWarnings("serial")
public class RelationalHierarchicalMappingPanel extends AbstractFlowStepAwareComponentEditPanel implements IMappingPanel {

    MappingDiagram diagram;

    Button removeButton;

    Checkbox srcMapFilter;

    Checkbox dstMapFilter;

    TextField srcTextFilter;

    TextField dstTextFilter;    
    
    RelationalModel inputModel;
    
    HierarchicalModel outputModel;
    
    Grid<EntitySettings> queryMappingGrid = new Grid<EntitySettings>();
    List<EntitySettings> entitySettings = new ArrayList<EntitySettings>();

    EditByQueryMappingDialog queryMappingDialog;

    protected void buildUI() {
        this.inputModel = (RelationalModel) component.getInputModel();
        this.outputModel = (HierarchicalModel) component.getOutputModel();
        
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            add(buttonBar);
            Button byQueryMapButton = buttonBar.addButton("By Query Map", VaadinIcon.MAP_MARKER);
            removeButton = buttonBar.addButton("Remove", VaadinIcon.TRASH);
            removeButton.setEnabled(false);
            byQueryMapButton.addClickListener(new ByQueryMapListener());            
            String queryMethod = component.get(RelationalHierarchicalMapping.HIERARCHICAL_QUERY_METHOD,RelationalHierarchicalMapping.QUERY_METHOD_BY_JOIN);
            if(queryMethod.equalsIgnoreCase(RelationalHierarchicalMapping.QUERY_METHOD_BY_JOIN)) {
                byQueryMapButton.setEnabled(false);
            }
            removeButton.addClickListener(new RemoveListener());
        }
        buttonBar.addButtonRight("Export", VaadinIcon.DOWNLOAD, (e)->export());

        HorizontalLayout titleHeader = new HorizontalLayout();
        titleHeader.setSpacing(true);
        titleHeader.getStyle().set("margin", "0 16px");
        titleHeader.setWidthFull();
		Html inputModelHtml = new Html("<b>Input Model:</b> &nbsp;"
				+ (component.getInputModel() != null ? component.getInputModel().getName() : "?"));
		Html outputModelHtml = new Html("<b>Output Model:</b> &nbsp;"
				+ (component.getOutputModel() != null ? component.getOutputModel().getName() : "?"));
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

        VerticalLayout vlay = new VerticalLayout();
        vlay.setSizeFull();
        diagram = new MappingDiagram(context, component, this);
        diagram.setSizeFull();
        vlay.add(diagram);
        add(vlay);
        expand(vlay);
        
        buildByQueryMappingDialog();
    }    
    
    protected void buildByQueryMappingDialog() {
        queryMappingDialog = new EditByQueryMappingDialog();
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
        Grid<Object> grid = new Grid<Object>();
        grid.addColumn(item -> "").setHeader("Source Entity");
        grid.addColumn(item -> "").setHeader("Source Attribute");
        grid.addColumn(item -> "").setHeader("Destination Schema Object");
        
        int itemId = 0;
        for (ComponentAttribSetting setting : component.getAttributeSettings()) {
            if (Mapping.ATTRIBUTE_MAPS_TO.equals(setting.getName())) {
//                ModelAttrib srcAttribute = component.getInputModel().getAttributeById(setting.getAttributeId());
//                ModelEntity srcEntity = component.getInputModel().getEntityById(srcAttribute.getEntityId());
//                ModelAttrib dstAttribute = component.getOutputModel().getAttributeById(setting.getValue());
//                ModelEntity dstEntity = component.getOutputModel().getEntityById(dstAttribute.getEntityId());
                
//                table.addItem(new Object[]{srcEntity.getName(), srcAttribute.getName(), dstEntity.getName(), dstAttribute.getName()}, itemId++);
            }
        }
        
        ExportDialog.show(context, grid);
    }
    
    @Override
    public void selectEvent(SelectEvent event) {
        removeButton.setEnabled(event.getSelectedSourceId() != null);
    }

    class RemoveListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            diagram.removeSelected();
            removeButton.setEnabled(false);
        }
    }

    class ByQueryMapListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            refreshEntitySettingsContainer();
            updateQueryMappingGrid();
            queryMappingDialog.show();

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
    
    class EditByQueryMappingDialog extends ResizableDialog {
        private static final long serialVersionUID = 1L;

        public EditByQueryMappingDialog() {
            super("Edit Entity Mapping to Source Reader");
            setWidth("800px");
            setHeight("600px");
            innerContent.setMargin(true);
            buildQueryMappingGrid();
            add(buildButtonFooter(buildCloseButton()));
        }

        private void buildQueryMappingGrid() {

            queryMappingGrid.setSizeFull();
            queryMappingGrid.addColumn(setting -> {
                HierarchicalModel model = (HierarchicalModel) component.getOutputModel();
                ModelSchemaObject object = model.getObjectById(setting.getEntityId());
                return object.getName();
            }).setHeader("Entiy Name").setFlexGrow(1).setSortable(false);
            queryMappingGrid.addComponentColumn(setting -> createSourceStepComboBox(setting, RelationalHierarchicalMapping.ENTITY_TO_ORGINATING_STEP_ID))
                    .setHeader("Source Step").setWidth("350px").setSortable(false);
            add(queryMappingGrid, 1);
        }
    }
    
    private void refreshEntitySettingsContainer() {
        entitySettings.clear();
        ModelSchemaObject root = outputModel.getRootObject();
        List<ComponentEntitySetting> compEntitySettings = component.getEntitySettings();
        Set<String> existingEntitySettings = new HashSet<String>();
        for (ComponentEntitySetting compEntitySetting : compEntitySettings) {
            if (RelationalHierarchicalMapping.ENTITY_TO_ORGINATING_STEP_ID.equalsIgnoreCase(compEntitySetting.getName())) {
                entitySettings.add(new EntitySettings(compEntitySetting.getEntityId(), compEntitySetting.getValue()));
                existingEntitySettings.add(compEntitySetting.getEntityId());
            }
        }
        addEntitySettings(root, existingEntitySettings);
    }
    
    private void addEntitySettings(ModelSchemaObject object, Set<String> existingEntitySettings) {
        if (!existingEntitySettings.contains(object.getId())) {
            entitySettings.add(new EntitySettings(object.getId(),null));
        }
        for (ModelSchemaObject childObject : object.getChildObjects()) {
            addEntitySettings(childObject, existingEntitySettings);
        }
    }
    
//        List<ModelEntity> entities = component.getOutputModel().getModelEntities();
//        List<ComponentEntitySetting> compEntitySettings = component.getEntitySettings();
//        Set<String> existingEntitySettings = new HashSet<String>();
//        for (ComponentEntitySetting compEntitySetting:compEntitySettings) {
//            if (RelationalHierarchicalMapping.ENTITY_TO_ORGINATING_STEP_ID.equalsIgnoreCase(compEntitySetting.getName())) {
//                entitySettings.add(new EntitySettings(compEntitySetting.getEntityId(),compEntitySetting.getValue()));
//                existingEntitySettings.add(compEntitySetting.getEntityId());
//            }
//        }        
//        for (ModelEntity entity:entities) {
//            if (!existingEntitySettings.contains(entity.getId())) {
//                entitySettings.add(new EntitySettings(entity.getId(),null));
//            }
//        }        


    protected void updateQueryMappingGrid() {
        queryMappingGrid.setItems(entitySettings);
    }    
    
    protected ComboBox<FlowStep> createSourceStepComboBox(final EntitySettings settings, final String key) {
        final ComboBox<FlowStep> comboBox = new ComboBox<FlowStep>();
        flow = context.getConfigurationService().findFlow(flow.getId());
        List<FlowStep> comboStepList = new ArrayList<FlowStep>();
        List<FlowStepLink> stepLinks = flow.findFlowStepLinksWithTarget(flowStep.getId());
        for (FlowStepLink flowStepLink : stepLinks) {
            FlowStep comboStep = flow.findFlowStepWithId(flowStepLink.getSourceStepId());
            comboStepList.add(comboStep);
        }
        comboBox.setItemLabelGenerator(item -> item.getName());
        comboBox.setItems(comboStepList);

        comboBox.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<FlowStep>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(ValueChangeEvent<FlowStep> event) {
                ComponentEntitySetting setting = component.getSingleEntitySetting(settings.getEntityId(), key);

                String oldValue = setting == null ? null : setting.getValue();
                if (setting == null) {
                    setting = new ComponentEntitySetting(settings.getEntityId(), component.getId(), key, null);
                    component.addEntitySetting(setting);
                }
                if (comboBox.getValue() != null && comboBox.getValue().getId() != null) {
                    setting.setValue(comboBox.getValue().getId().toString());
                } else {
                    setting.setValue(null);
                }
                if (oldValue == null || !oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
        comboBox.setReadOnly(readOnly);
        comboBox.setWidth("100%");
        return comboBox;
    }

    
    public static class EntitySettings implements Serializable {
        private static final long serialVersionUID = 1L;
        String entityId;
        String sourceStep;

        public EntitySettings(String entityId, String sourceStep) {
            this.entityId = entityId;
            this.sourceStep = sourceStep;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public String getSourceStep() {
            return sourceStep;
        }

        public void setSourceStep(String sourceStep) {
            this.sourceStep = sourceStep;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EntitySettings) {
                return entityId.equals(((EntitySettings) obj).getEntityId());
            } else {
                return super.equals(obj);
            }
        }
    }
}
