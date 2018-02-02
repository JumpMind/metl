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

import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ExportDialog extends ResizableWindow {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    private ApplicationContext context;
    OptionGroup exportFlowGroup;
    OptionGroup exportModelGroup;
    OptionGroup exportResourceGroup;
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
        exportFlowGroup.setValue(exportFlowGroup.getContainerDataSource().getItemIds());
        exportModelGroup.setValue(exportModelGroup.getContainerDataSource().getItemIds());
        exportResourceGroup.setValue(exportResourceGroup.getContainerDataSource().getItemIds());
    }
    
    private void selectNone() {
        exportFlowGroup.setValue(null);
        exportModelGroup.setValue(null);
        exportResourceGroup.setValue(null);
    }

    @SuppressWarnings("unchecked")
    private void updateAffectedObjects() {
        Set<String> flowIds = (Set<String>) exportFlowGroup.getValue();
        Set<String> modelIds = (Set<String>) exportModelGroup.getValue();
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
        ModelName selectedModel = null;
        ResourceName selectedResource = null;
        boolean allChecked = false;
        if (selected instanceof ProjectVersion) {
            ProjectVersion project = (ProjectVersion) selected;
            projectVersionId = project.getId();
            allChecked = true;
        } else if (selected instanceof FlowName) {
            selectedFlow = (FlowName) selected;
            projectVersionId = selectedFlow.getProjectVersionId();
        } else if (selected instanceof ModelName) {
            selectedModel = (ModelName) selected;
            projectVersionId = selectedModel.getProjectVersionId();
        } else if (selected instanceof ResourceName) {
            selectedResource = (ResourceName) selected;
            projectVersionId = selectedResource.getProjectVersionId();
        }
        List<FlowName> allFlows = configurationService.findFlowsInProject(projectVersionId, false);
        allFlows.addAll(configurationService.findFlowsInProject(projectVersionId, true));
        AbstractObjectNameBasedSorter.sort(allFlows);

        // flows
        exportFlowGroup = new OptionGroup("Flows");
        exportFlowGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        exportFlowGroup.setMultiSelect(true);
        for (FlowName key : allFlows) {
            exportFlowGroup.addItem(key.getId());
            exportFlowGroup.setItemCaption(key.getId(), key.getName());
            if (allChecked || key.equals(selectedFlow)) {
                exportFlowGroup.select(key.getId());
            }
        }
        exportFlowGroup.addValueChangeListener(selectedItem -> updateAffectedObjects());
        layout.addComponent(exportFlowGroup);

        // models
        List<ModelName> models = configurationService.findModelsInProject(projectVersionId);
        AbstractObjectNameBasedSorter.sort(models);
        exportModelGroup = new OptionGroup("Models");
        exportModelGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        exportModelGroup.setMultiSelect(true);
        for (ModelName key : models) {
            exportModelGroup.addItem(key.getId());
            exportModelGroup.setItemCaption(key.getId(), key.getName());
            if (allChecked || key.equals(selectedModel)) {
                exportModelGroup.select(key.getId());
            }
        }
        layout.addComponent(exportModelGroup);

        // resources
        List<ResourceName> resources = configurationService.findResourcesInProject(projectVersionId);
        AbstractObjectNameBasedSorter.sort(resources);
        exportResourceGroup = new OptionGroup("Resources");
        exportResourceGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        exportResourceGroup.setMultiSelect(true);
        for (ResourceName key : resources) {
            exportResourceGroup.addItem(key.getId());
            exportResourceGroup.setItemCaption(key.getId(), key.getName());
            if (allChecked || key.equals(selectedResource)) {
                exportResourceGroup.select(key.getId());
            }
        }
        layout.addComponent(exportResourceGroup);

        @SuppressWarnings("unchecked")
        Set<String> flowIds = (Set<String>) exportFlowGroup.getValue();
        for (String flowId : flowIds) {
            addDependentModels(flowId);
            addDependentResources(flowId);
        }

    }

    private void addDependentModels(String flowId) {
        List<Model> models = context.getConfigurationService().findDependentModels(flowId);
        for (Model model : models) {
            exportModelGroup.addItem(model.getId());
            exportModelGroup.setItemCaption(model.getId(), model.getName());
            exportModelGroup.select(model.getId());
        }
    }

    private void addDependentResources(String flowId) {
        List<Resource> resources = context.getConfigurationService().findDependentResources(flowId);
        for (Resource resource : resources) {
            exportResourceGroup.addItem(resource.getId());
            exportResourceGroup.setItemCaption(resource.getId(), resource.getName());
            exportResourceGroup.select(resource.getId());
        }
    }

    public static interface IExportListener extends Serializable {
        public void onFinished(String dataToImport);
    }

    @SuppressWarnings({ "serial", "unchecked" })
    class ExportClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<String> flowIds = (Set<String>) exportFlowGroup.getValue();
            Set<String> modelIds = (Set<String>) exportModelGroup.getValue();
            Set<String> resourceIds = (Set<String>) exportResourceGroup.getValue();
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
