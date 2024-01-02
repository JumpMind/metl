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
package org.jumpmind.metl.ui.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.RelationalModelName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.provider.Query;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ExportDialog extends ResizableWindow {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    private ApplicationContext context;
    CheckBoxGroup<FlowName> exportFlowGroup;
    CheckBoxGroup<RelationalModelName> exportModelGroup;
    CheckBoxGroup<ResourceName> exportResourceGroup;
    VerticalLayout affectedLayout;
    String projectVersionId;

    public ExportDialog(ApplicationContext context, Object selectedElement) {
        super("Export Configuration");
        this.context = context;
        initWindow(selectedElement);
    }

    private void initWindow(Object selectedItem) {
        Panel exportPanel = new Panel("Export and Dependencies");
        exportPanel.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        exportPanel.setSizeFull();
        VerticalLayout exportLayout = new VerticalLayout();
        exportLayout.setMargin(true);
        addSelectedAndDependentObjects(exportLayout, selectedItem);
        exportPanel.setContent(exportLayout);

        Panel affectedPanel = new Panel("Possible Affected Flows");
        affectedPanel.setSizeFull();
        exportPanel.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        affectedLayout = new VerticalLayout();
        affectedLayout.setMargin(true);
        updateAffectedObjects();
        affectedPanel.setContent(affectedLayout);

        // Split panel for Export and Affected
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setWidth(100, Unit.PERCENTAGE);
        splitPanel.setFirstComponent(exportPanel);
        splitPanel.setSecondComponent(affectedPanel);

        addComponent(splitPanel, 1);
        
        Button selectAllLink = new Button("Select All");
        selectAllLink.addStyleName(ValoTheme.BUTTON_LINK);
        selectAllLink.addClickListener((event) -> selectAll());

        Button selectNoneLink = new Button("Select None");
        selectNoneLink.addStyleName(ValoTheme.BUTTON_LINK);
        selectNoneLink.addClickListener((event) -> selectNone());
        
        addComponent(buildButtonFooter(new Button[] {selectAllLink, selectNoneLink}, new Button("Export", new ExportClickListener()), buildCloseButton()));

        setWidth(700, Unit.PIXELS);
        setHeight(500, Unit.PIXELS);
    }
    
    private void selectAll() {
		exportFlowGroup.setValue(exportFlowGroup.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet()));
        exportModelGroup.setValue(exportModelGroup.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet()));
        exportResourceGroup.setValue(exportResourceGroup.getDataProvider().fetch(new Query<>()).collect(Collectors.toSet()));
    }
    
    private void selectNone() {
        exportFlowGroup.setValue(null);
        exportModelGroup.setValue(null);
        exportResourceGroup.setValue(null);
    }

    private void updateAffectedObjects() {
        Set<String> flowIds = exportFlowGroup.getValue().stream().map(item -> item.getId()).collect(Collectors.toSet());
        Set<String> modelIds = exportModelGroup.getValue().stream().map(item -> item.getId()).collect(Collectors.toSet());
        affectedLayout.removeAllComponents();
        Set<Flow> flows = new HashSet<Flow>();
        for (String flowId : flowIds) {
            flows.addAll(context.getConfigurationService().findAffectedFlowsByFlow(flowId));
        }
        for (String modelId : modelIds) {
            flows.addAll(context.getConfigurationService().findAffectedFlowsByModel(modelId));
        }

        for (Flow flow : flows) {
            // only add flow to affected flows if its not already being exported
            if (!flowIds.contains(flow.getId())) {
                affectedLayout.addComponent(new Label(" - " + flow.getName()));
            }
        }
    }

    private void addSelectedAndDependentObjects(VerticalLayout layout, Object selected) {
        IConfigurationService configurationService = context.getConfigurationService();
        FlowName selectedFlow = null;
        RelationalModelName selectedModel = null;
        ResourceName selectedResource = null;
        boolean allChecked = false;
        if (selected instanceof ProjectVersion) {
            ProjectVersion project = (ProjectVersion) selected;
            projectVersionId = project.getId();
            allChecked = true;
        } else if (selected instanceof FlowName) {
            selectedFlow = (FlowName) selected;
            projectVersionId = selectedFlow.getProjectVersionId();
        } else if (selected instanceof RelationalModelName) {
            selectedModel = (RelationalModelName) selected;
            projectVersionId = selectedModel.getProjectVersionId();
        } else if (selected instanceof ResourceName) {
            selectedResource = (ResourceName) selected;
            projectVersionId = selectedResource.getProjectVersionId();
        }
        List<FlowName> allFlows = configurationService.findFlowsInProject(projectVersionId, false);
        allFlows.addAll(configurationService.findFlowsInProject(projectVersionId, true));
        AbstractObjectNameBasedSorter.sort(allFlows);

        // flows
        exportFlowGroup = new CheckBoxGroup<FlowName>("Flows");
        exportFlowGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        exportFlowGroup.setItems(allFlows);
        exportFlowGroup.setItemCaptionGenerator(item -> item.getName());
        for (FlowName key : allFlows) {
            if (allChecked || key.equals(selectedFlow)) {
                exportFlowGroup.select(key);
            }
        }
        exportFlowGroup.addValueChangeListener(selectedItem -> updateAffectedObjects());
        layout.addComponent(exportFlowGroup);

        // models
        List<RelationalModelName> models = configurationService.findRelationalModelsInProject(projectVersionId);
        AbstractObjectNameBasedSorter.sort(models);
        exportModelGroup = new CheckBoxGroup<RelationalModelName>("Models");
        exportModelGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        exportModelGroup.setItemCaptionGenerator(item -> item.getName());
        layout.addComponent(exportModelGroup);

        // resources
        List<ResourceName> resources = configurationService.findResourcesInProject(projectVersionId);
        AbstractObjectNameBasedSorter.sort(resources);
        exportResourceGroup = new CheckBoxGroup<ResourceName>("Resources");
        exportResourceGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        exportResourceGroup.setItemCaptionGenerator(item -> item.getName());
        layout.addComponent(exportResourceGroup);

        List<RelationalModelName> dependentModels = new ArrayList<RelationalModelName>();
        List<ResourceName> dependentResources = new ArrayList<ResourceName>();
        Set<FlowName> flowNames = exportFlowGroup.getValue();
        for (FlowName flowName : flowNames) {
        	String flowId = flowName.getId();
			dependentModels.addAll(context.getConfigurationService().findDependentModels(flowId).stream()
					.map(model -> new RelationalModelName(model)).collect(Collectors.toList()));
			dependentResources.addAll(context.getConfigurationService().findDependentResources(flowId).stream()
					.map(resource -> new ResourceName(resource)).collect(Collectors.toList()));
        }
        
        models.addAll(dependentModels);
        exportModelGroup.setItems(models);
        for (RelationalModelName key : models) {
            if (allChecked || key.equals(selectedModel) || dependentModels.contains(key)) {
                exportModelGroup.select(key);
            }
        }
        
        resources.addAll(dependentResources);
        exportResourceGroup.setItems(resources);
        for (ResourceName key : resources) {
            if (allChecked || key.equals(selectedResource) || dependentResources.contains(key)) {
                exportResourceGroup.select(key);
            }
        }

    }

    public static interface IExportListener extends Serializable {
        public void onFinished(String dataToImport);
    }

    @SuppressWarnings({ "serial" })
    class ExportClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<String> flowIds = exportFlowGroup.getValue().stream().map(item -> item.getId()).collect(Collectors.toSet());
            Set<String> modelIds = exportModelGroup.getValue().stream().map(item -> item.getId()).collect(Collectors.toSet());
            Set<String> resourceIds = exportResourceGroup.getValue().stream().map(item -> item.getId()).collect(Collectors.toSet());
            String export = context.getImportExportService().exportFlows(projectVersionId, new ArrayList<String>(flowIds),
                    new ArrayList<String>(modelIds), new ArrayList<String>(resourceIds), context.getUser().getLoginId());
            ProjectVersion projectVersion = context.getConfigurationService().findProjectVersion(projectVersionId);
            downloadExport(export, String.format("%s-%s", projectVersion.getProject().getName(), projectVersion.getName()).toLowerCase().replaceAll(" - ", " ").replaceAll(" ", "-"));

        }
    }

    protected void downloadExport(final String export, String filename) {

        StreamSource ss = new StreamSource() {
            private static final long serialVersionUID = 1L;

            public InputStream getStream() {
                try {
                    return new ByteArrayInputStream(export.getBytes(Charset.forName("utf-8")));
                } catch (Exception e) {
                    log.error("Failed to export configuration", e);
                    CommonUiUtils.notify("Failed to export configuration.", Type.ERROR_MESSAGE);
                    return null;
                }
            }
        };
        String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        StreamResource resource = new StreamResource(ss, String.format("%s-config-%s.json", filename, datetime));
        final String KEY = "export";
        setResource(KEY, resource);
        Page.getCurrent().open(ResourceReference.create(resource, this, KEY).getURL(), null);
    }

    public static void show(ApplicationContext context, Object selectedElement) {
        ExportDialog dialog = new ExportDialog(context, selectedElement);
        UI.getCurrent().addWindow(dialog);
    }
}
