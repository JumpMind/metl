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
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.Mapping;
import org.jumpmind.metl.core.runtime.component.RelationalHierarchicalMapping;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.views.design.AbstractFlowStepAwareComponentEditPanel;
import org.jumpmind.vaadin.ui.common.ExportDialog;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditMappingPanel extends AbstractFlowStepAwareComponentEditPanel {

    MappingDiagram diagram;

    Button removeButton;

    CheckBox srcMapFilter;

    CheckBox dstMapFilter;

    TextField srcTextFilter;

    TextField dstTextFilter;    
    
    Table queryMappingTable = new Table();
    BeanItemContainer<EntitySettings> entitySettingsContainer = new BeanItemContainer<EntitySettings>(EntitySettings.class);
    List<EntitySettings> entitySettings = new ArrayList<EntitySettings>();

    EditByQueryMappingWindow queryMappingWindow;

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            addComponent(buttonBar);
            Button autoMapButton = buttonBar.addButton("Auto Map", FontAwesome.FLASH);
            Button byQueryMapButton = buttonBar.addButton("By Query Map", FontAwesome.MAP);
            removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
            removeButton.setEnabled(false);
            byQueryMapButton.addClickListener(new ByQueryMapListener());            
            autoMapButton.addClickListener(new AutoMapListener());
            String queryMethod = component.get(RelationalHierarchicalMapping.HIERARCHICAL_QUERY_METHOD,RelationalHierarchicalMapping.QUERY_METHOD_BY_JOIN);
            if(queryMethod.equalsIgnoreCase(RelationalHierarchicalMapping.QUERY_METHOD_BY_JOIN)) {
                byQueryMapButton.setEnabled(false);
            }
            removeButton.addClickListener(new RemoveListener());
        }
        buttonBar.addButtonRight("Export", FontAwesome.DOWNLOAD, (e)->export());

        HorizontalLayout titleHeader = new HorizontalLayout();
        titleHeader.setSpacing(true);
        titleHeader.setMargin(new MarginInfo(false, true, false, true));
        titleHeader.setWidth(100f, Unit.PERCENTAGE);
        titleHeader.addComponent(
                new Label("<b>Input Model:</b> &nbsp;" + (component.getInputModel() != null ? component.getInputModel().getName() : "?"),
                        ContentMode.HTML));
        titleHeader.addComponent(
                new Label("<b>Output Model:</b> &nbsp;" + (component.getOutputModel() != null ? component.getOutputModel().getName() : "?"),
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
        
        buildByQueryMappingWindow();
    }    
    
    protected void buildByQueryMappingWindow() {
        queryMappingWindow = new EditByQueryMappingWindow();
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
        for (ModelEntity entity1 : component.getInputModel().getModelEntities()) {
            for (ModelAttrib attr : entity1.getModelAttributes()) {
                /* look for exact match first */
                for (ModelEntity entity2 : component.getOutputModel().getModelEntities()) {
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
        for (ComponentAttribSetting setting : component.getAttributeSettings()) {
            if (setting.getName().equals(Mapping.ATTRIBUTE_MAPS_TO) && setting.getValue().equals(attr2.getId())) {
                isMapped = true;
                break;
            }
        }
        if (!isMapped && ((fuzzy && fuzzyMatches(attr.getName(), attr2.getName()))
                || ((!exact && attr.getName().equalsIgnoreCase(attr2.getName())) || exactMatch))) {
            ComponentAttribSetting setting = new ComponentAttribSetting();
            setting.setAttributeId(attr.getId());
            setting.setComponentId(component.getId());
            setting.setName(Mapping.ATTRIBUTE_MAPS_TO);
            setting.setValue(attr2.getId());
            component.addAttributeSetting(setting);
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
        Table table = new Table();
        table.addContainerProperty("Source Entity", String.class, null);
        table.addContainerProperty("Source Attribute",  String.class, null);
        table.addContainerProperty("Destination Entity", String.class, null);
        table.addContainerProperty("Destination Attribute",  String.class, null);
        
        int itemId = 0;
        for (ComponentAttribSetting setting : component.getAttributeSettings()) {
            if (Mapping.ATTRIBUTE_MAPS_TO.equals(setting.getName())) {
                ModelAttrib srcAttribute = component.getInputModel().getAttributeById(setting.getAttributeId());
                ModelEntity srcEntity = component.getInputModel().getEntityById(srcAttribute.getEntityId());
                ModelAttrib dstAttribute = component.getOutputModel().getAttributeById(setting.getValue());
                ModelEntity dstEntity = component.getOutputModel().getEntityById(dstAttribute.getEntityId());
                
                table.addItem(new Object[]{srcEntity.getName(), srcAttribute.getName(), dstEntity.getName(), dstAttribute.getName()}, itemId++);
            }
        }
        
        String fileNamePrefix = component.getName().toLowerCase().replace(' ', '-');
        ExportDialog dialog = new ExportDialog(table, fileNamePrefix, component.getName());
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
    
    class ByQueryMapListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            //TODO:
            refreshEntitySettingsContainer();
            updateQueryMappingTable();
            queryMappingWindow.show();

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
    
    class EditByQueryMappingWindow extends ResizableWindow {
        private static final long serialVersionUID = 1L;

        public EditByQueryMappingWindow() {
            super("Edit Entity Mapping to Source Reader");
            setWidth(800f, Unit.PIXELS);
            setHeight(600f, Unit.PIXELS);
            content.setMargin(true);
            buildQueryMappingTable();
            addComponent(buildButtonFooter(buildCloseButton()));
        }

        private void buildQueryMappingTable() {

            queryMappingTable.setContainerDataSource(entitySettingsContainer);
            queryMappingTable.setSelectable(true);
            queryMappingTable.setSortEnabled(false);
            queryMappingTable.setImmediate(true);
            queryMappingTable.setSizeFull();
            queryMappingTable.addGeneratedColumn("entityName", new ColumnGenerator() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
                    EntitySettings setting = (EntitySettings) itemId;
                    Model model = component.getOutputModel();
                    ModelEntity entity = model.getEntityById(setting.getEntityId());
                    return entity.getName();
                }
            });
            queryMappingTable.setVisibleColumns(new Object[] { "entityName", "sourceStep" });
            queryMappingTable.setColumnWidth("sourceStep", 350);
            queryMappingTable.setColumnHeaders(new String[] { "Entity Name", "Source Step" });
            queryMappingTable.setColumnExpandRatio("entityName", 1);
            queryMappingTable.setTableFieldFactory(new EditSourceStepFieldFactory());
            queryMappingTable.setEditable(true);
            addComponent(queryMappingTable, 1);
        }
    }
    
    private void refreshEntitySettingsContainer() {
        entitySettings.clear();
        List<ModelEntity> entities = component.getOutputModel().getModelEntities();
        List<ComponentEntitySetting> compEntitySettings = component.getEntitySettings();
        Set<String> existingEntitySettings = new HashSet<String>();
        for (ComponentEntitySetting compEntitySetting:compEntitySettings) {
            if (RelationalHierarchicalMapping.ENTITY_TO_ORGINATING_STEP_ID.equalsIgnoreCase(compEntitySetting.getName())) {
                entitySettings.add(new EntitySettings(compEntitySetting.getEntityId(),compEntitySetting.getValue()));
                existingEntitySettings.add(compEntitySetting.getEntityId());
            }
        }        
        for (ModelEntity entity:entities) {
            if (!existingEntitySettings.contains(entity.getId())) {
                entitySettings.add(new EntitySettings(entity.getId(),null));
            }
        }        
    }

    protected void updateQueryMappingTable() {
        queryMappingTable.removeAllItems();
        for (EntitySettings entitySetting : entitySettings) {
            queryMappingTable.addItem(entitySetting);
        }
    }    
    
    class EditSourceStepFieldFactory implements TableFieldFactory {
        private static final long serialVersionUID = 1L;

        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final EntitySettings settings = (EntitySettings) itemId;
            if (propertyId.equals("sourceStep")) {
                return createSourceStepComboBox(settings, RelationalHierarchicalMapping.ENTITY_TO_ORGINATING_STEP_ID);
            } else {
                return null;
            }
        }
    }
    
    protected ComboBox createSourceStepComboBox(final EntitySettings settings, final String key) {
        final ComboBox comboBox = new ComboBox();
        comboBox.setImmediate(true);
        flow = context.getConfigurationService().findFlow(flow.getId());
        List<FlowStepLink> stepLinks = flow.findFlowStepLinksWithTarget(flowStep.getId());
        for (FlowStepLink flowStepLink : stepLinks) {
            FlowStep comboStep = flow.findFlowStepWithId(flowStepLink.getSourceStepId());
            comboBox.addItem(comboStep.getId());
            comboBox.setItemCaption(comboStep.getId(), comboStep.getName());
        }

        comboBox.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                ComponentEntitySetting setting = component.getSingleEntitySetting(settings.getEntityId(), key);

                String oldValue = setting == null ? null : setting.getValue();
                if (setting == null) {
                    setting = new ComponentEntitySetting(settings.getEntityId(), component.getId(), key, null);
                    component.addEntitySetting(setting);
                }
                setting.setValue(comboBox.getValue().toString());
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
