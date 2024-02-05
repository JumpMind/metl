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
import org.jumpmind.vaadin.ui.common.ResizableDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;

public class ExportDialog extends ResizableDialog {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    private ApplicationContext context;
    Anchor exportAnchor;
    boolean generateNewExport = true;
    CheckboxGroup<FlowName> exportFlowGroup;
    CheckboxGroup<RelationalModelName> exportModelGroup;
    CheckboxGroup<ResourceName> exportResourceGroup;
    VerticalLayout affectedLayout;
    String projectVersionId;

    public ExportDialog(ApplicationContext context, Object selectedElement) {
        super("Export Configuration");
        this.context = context;
        initDialog(selectedElement);
    }

    private void initDialog(Object selectedItem) {
        H3 exportHeader = new H3("Export and Dependencies");
        VerticalLayout exportLayout = new VerticalLayout();
        exportLayout.setMargin(true);
        exportLayout.setSizeFull();
        addSelectedAndDependentObjects(exportLayout, selectedItem);

        H3 affectedHeader = new H3("Possible Affected Flows");
        affectedLayout = new VerticalLayout();
        affectedLayout.setMargin(true);
        affectedLayout.setSizeFull();
        updateAffectedObjects();

        // Split layout for Export and Affected
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setWidthFull();
        splitLayout.addToPrimary(exportHeader, exportLayout);
        splitLayout.addToSecondary(affectedHeader, affectedLayout);

        add(splitLayout, 1);
        
        Button selectAllLink = new Button("Select All");
        selectAllLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        selectAllLink.addClickListener((event) -> selectAll());

        Button selectNoneLink = new Button("Select None");
        selectNoneLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        selectNoneLink.addClickListener((event) -> selectNone());
        
        exportAnchor = new Anchor();
        exportAnchor.setTarget("_blank");
        exportAnchor.getElement().setAttribute("download", true);
        Button exportButton = new Button("Export", new ExportClickListener());
        exportAnchor.add(exportButton);
        add(buildButtonFooter(new Button[] {selectAllLink, selectNoneLink}, exportAnchor, buildCloseButton()));

        setWidth("700px");
        setHeight("500px");
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
        affectedLayout.removeAll();
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
                affectedLayout.add(new Span(" - " + flow.getName()));
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
        exportFlowGroup = new CheckboxGroup<FlowName>();
        exportFlowGroup.setLabel("Flows");
        exportFlowGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        exportFlowGroup.setItems(allFlows);
        exportFlowGroup.setItemLabelGenerator(item -> item.getName());
        for (FlowName key : allFlows) {
            if (allChecked || key.equals(selectedFlow)) {
                exportFlowGroup.select(key);
            }
        }
        exportFlowGroup.addValueChangeListener(selectedItem -> updateAffectedObjects());
        layout.add(exportFlowGroup);

        // models
        List<RelationalModelName> models = configurationService.findRelationalModelsInProject(projectVersionId);
        AbstractObjectNameBasedSorter.sort(models);
        exportModelGroup = new CheckboxGroup<RelationalModelName>();
        exportModelGroup.setLabel("Models");
        exportModelGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        exportModelGroup.setItemLabelGenerator(item -> item.getName());
        layout.add(exportModelGroup);

        // resources
        List<ResourceName> resources = configurationService.findResourcesInProject(projectVersionId);
        AbstractObjectNameBasedSorter.sort(resources);
        exportResourceGroup = new CheckboxGroup<ResourceName>();
        exportResourceGroup.setLabel("Resources");
        exportResourceGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        exportResourceGroup.setItemLabelGenerator(item -> item.getName());
        layout.add(exportResourceGroup);

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
    class ExportClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
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
        if (generateNewExport) {
            InputStreamFactory factory = new InputStreamFactory() {
                private static final long serialVersionUID = 1L;

                public InputStream createInputStream() {
                    try {
                        return new ByteArrayInputStream(export.getBytes(Charset.forName("utf-8")));
                    } catch (Exception e) {
                        log.error("Failed to export configuration", e);
                        CommonUiUtils.notifyError("Failed to export configuration.");
                        return null;
                    }
                }
            };
            String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            StreamResource resource = new StreamResource(String.format("%s-config-%s.json", filename, datetime), factory);
            exportAnchor.setHref(resource);
            generateNewExport = false;
            UI.getCurrent().getPage().executeJs("$0.click();", exportAnchor.getElement());
        } else {
            exportAnchor.removeHref();
            generateNewExport = true;
        }
    }

    public static void show(ApplicationContext context, Object selectedElement) {
        new ExportDialog(context, selectedElement).open();
    }
}
