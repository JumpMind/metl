package org.jumpmind.metl.ui.views;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class ExportDialog extends Window {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    private ApplicationContext context;
    private Map<String, String> flowMap;
    private Map<String, String> modelMap;
    private Map<String, String> resourceMap;
    private String projectVersionId;
    OptionGroup exportFlowGroup;

    public ExportDialog(ApplicationContext context, Object selectedElement) {

        this.context = context;
        this.flowMap = new HashMap<String, String>();
        this.modelMap = new HashMap<String, String>();
        this.resourceMap = new HashMap<String, String>();
        initWindow(selectedElement);
    }

    private void initWindow(Object selectedItem) {

        // button bar
        ButtonBar buttonBar = new ButtonBar();
        Button exportButton = new Button("Export");
        exportButton.addClickListener(new ExportClickListener());
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new CancelClickListener());
        buttonBar.addComponent(cancelButton);
        buttonBar.addComponent(exportButton);

        // export panel
        Panel exportPanel = new Panel("Export and Dependencies");
        VerticalLayout exportLayout = new VerticalLayout();
        exportLayout.setMargin(true);
        addSelectedAndDependentObjects(exportLayout, selectedItem);
        exportPanel.setContent(exportLayout);

        // affected panel
        Panel affectedPanel = new Panel("Possible Affected Flows");
        VerticalLayout affectedLayout = new VerticalLayout();
        affectedLayout.setMargin(true);
        addAffectedObjects(affectedLayout);
        affectedPanel.setContent(affectedLayout);

        // Split panel for Export and Affected
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setWidth(100, Unit.PERCENTAGE);
        splitPanel.setFirstComponent(exportPanel);
        splitPanel.setSecondComponent(affectedPanel);

        // vertical layout for the page
        VerticalLayout pageLayout = new VerticalLayout();
        pageLayout.setCaption("Export Configuration");
        pageLayout.addComponent(splitPanel);
        pageLayout.addComponent(buttonBar);

        // the window
        setCaption("File Export");
        setWidth(700, Unit.PIXELS);
        setHeight(500, Unit.PIXELS);
        setContent(pageLayout);

    }

    private void addAffectedObjects(VerticalLayout layout) {

        Set<Flow> flows = new HashSet<Flow>();
        for (String flowId : flowMap.values()) {
            flows.addAll(context.getConfigurationService().findAffectedFlowsByModel(flowId));
        }
        for (String modelId : modelMap.values()) {
            flows.addAll(context.getConfigurationService().findAffectedFlowsByModel(modelId));
        }

        for (Flow flow : flows) {
            // only add flow to affected flows if its not already being exported
            if (flowMap.get(flow.getName()) == null) {
                layout.addComponent(new Label(" - " + flow.getName()));
            }
        }
    }

    private void flowChanged() {
        // TODO:
    }

    private void addSelectedAndDependentObjects(VerticalLayout layout, Object selected) {

        if (selected instanceof ProjectVersion) {
            ProjectVersion project = (ProjectVersion) selected;
            projectVersionId = project.getId();
            List<Flow> flows = context.getConfigurationService()
                    .findDependentFlows(project.getId());
            for (Flow flow : flows) {
                flowMap.put(flow.getName(), flow.getId());
                addDependentModels(layout, flow.getId());
                addDependentResources(layout, flow.getId());
            }
        } else if (selected instanceof FlowName) {
            FlowName flowName = (FlowName) selected;
            projectVersionId = flowName.getProjectVersionId();
            flowMap.put(flowName.getName(), flowName.getId());
            addDependentModels(layout, flowName.getId());
            addDependentResources(layout, flowName.getId());
        } else if (selected instanceof ModelName) {
            ModelName modelName = (ModelName) selected;
            projectVersionId = modelName.getProjectVersionId();
            modelMap.put(modelName.getName(), modelName.getId());
        } else if (selected instanceof ResourceName) {
            ResourceName resourceName = (ResourceName) selected;
            projectVersionId = resourceName.getProjectVersionId();
            resourceMap.put(resourceName.getName(), resourceName.getId());
        }

        // flows
        exportFlowGroup = new OptionGroup("Flows");
        exportFlowGroup.setMultiSelect(true);
        exportFlowGroup.addItems(flowMap.keySet());
        for (String key : flowMap.keySet()) {
            exportFlowGroup.select(key);
        }
        exportFlowGroup.addValueChangeListener(selectedItem -> flowChanged());
        layout.addComponent(exportFlowGroup);

        // models
        OptionGroup exportModelGroup = new OptionGroup("Models");
        exportModelGroup.setMultiSelect(true);
        exportModelGroup.addItems(modelMap.keySet());
        for (String key : modelMap.keySet()) {
            exportModelGroup.select(key);
        }
        layout.addComponent(exportModelGroup);

        // resources
        OptionGroup exportResourceGroup = new OptionGroup("Resources");
        exportResourceGroup.setMultiSelect(true);
        exportResourceGroup.addItems(resourceMap.keySet());
        for (String key : resourceMap.keySet()) {
            exportResourceGroup.select(key);
        }
        layout.addComponent(exportResourceGroup);

    }

    private void addDependentModels(VerticalLayout layout, String flowId) {

        List<Model> models = context.getConfigurationService().findDependentModels(flowId);
        for (Model model : models) {
            modelMap.put(model.getName(), model.getId());
        }
    }

    private void addDependentResources(VerticalLayout layout, String flowId) {

        List<Resource> resources = context.getConfigurationService().findDependentResources(flowId);
        for (Resource resource : resources) {
            resourceMap.put(resource.getName(), resource.getId());
        }
    }

    public static interface IExportListener extends Serializable {
        public void onFinished(String dataToImport);
    }

    @SuppressWarnings("serial")
    class ExportClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            String export = context.getImportExportService().export(projectVersionId,
                    new ArrayList<String>(flowMap.values()),
                    new ArrayList<String>(modelMap.values()),
                    new ArrayList<String>(resourceMap.values()));
            ProjectVersion project = context.getConfigurationService()
                    .findProjectVersion(projectVersionId);
            downloadExport(export,
                    project.getName().toLowerCase().replaceAll(" - ", " ").replaceAll(" ", "-"));

        }
    }

    @SuppressWarnings("serial")
    class CancelClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            UI.getCurrent().removeWindow(ExportDialog.this);
        }
    }

    protected void downloadExport(final String export, String filename) {

        StreamSource ss = new StreamSource() {
            private static final long serialVersionUID = 1L;

            public InputStream getStream() {
                try {
                    return new ByteArrayInputStream(export.getBytes());
                } catch (Exception e) {
                    log.error("Failed to export configuration", e);
                    CommonUiUtils.notify("Failed to export configuration.", Type.ERROR_MESSAGE);
                    return null;
                }
            }
        };
        String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        StreamResource resource = new StreamResource(ss,
                String.format("%s-config-%s.json", filename, datetime));
        final String KEY = "export";
        setResource(KEY, resource);
        Page.getCurrent().open(ResourceReference.create(resource, this, KEY).getURL(), null);
        // UI.getCurrent().removeWindow(ExportDialog.this);
    }

    public static void show(ApplicationContext context, Object selectedElement) {
        ExportDialog dialog = new ExportDialog(context, selectedElement);
        UI.getCurrent().addWindow(dialog);
    }
}
