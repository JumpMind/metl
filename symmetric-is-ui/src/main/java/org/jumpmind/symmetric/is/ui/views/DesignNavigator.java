package org.jumpmind.symmetric.is.ui.views;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.resource.db.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedApplicationPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

@SuppressWarnings("serial")
public class DesignNavigator extends AbstractFolderNavigator {

    MenuItem newFlow;
    MenuItem newResource;
    MenuItem newModel;
    MenuItem run;

    TabbedApplicationPanel tabs;
    DesignComponentPalette designComponentPalette;
    DesignPropertySheet designPropertySheet;

    public DesignNavigator(FolderType folderType, IConfigurationService configurationService,
            TabbedApplicationPanel tabs, DesignComponentPalette designComponentPalette,
            DesignPropertySheet designPropertySheet) {
        super(folderType, configurationService);
        this.designComponentPalette = designComponentPalette;
        this.designPropertySheet = designPropertySheet;
        this.tabs = tabs;
    }

    protected void addMenuButtons(MenuBar leftMenuBar, MenuBar rightMenuBar) {
        newFlow = leftMenuBar.addItem("", Icons.FLOW, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewFlow();
            }
        });
        newFlow.setDescription("Add Flow");

        newResource = leftMenuBar.addItem("", Icons.GENERAL_RESOURCE, null);
        newResource.setDescription("Add Resource");

        MenuItem newDbResource = newResource.addItem("Add Database", Icons.DATABASE,
                new Command() {

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                    }
                });
        newDbResource.setDescription("Add Database Resource");

        newModel = leftMenuBar.addItem("", Icons.MODEL, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newModel.setDescription("Add Model");
        
        run = leftMenuBar.addItem("", Icons.RUN, new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                System.out.println("Hi!");
                openExecution(selectedItem);
            }
        });
        run.setDescription("Run on local agent");
    }

    @Override
    protected void openItem(Object item) {
        if (item instanceof Flow) {
            item = ((Flow) item).getLatestFlowVersion();
        }

        if (item instanceof FlowVersion) {
            FlowVersion flowVersion = (FlowVersion) item;
            DesignFlowLayout flowLayout = new DesignFlowLayout(configurationService, flowVersion,
                    designComponentPalette, designPropertySheet, this);
            tabs.addCloseableTab(
                    flowVersion.getFlow().getName() + " " + flowVersion.getName(),
                    Icons.FLOW, flowLayout);
        }
    }

    protected void openExecution(Object item) {
        if (item instanceof ComponentFlow) {
            item = ((ComponentFlow) item).getLatestComponentFlowVersion();
        }

        if (item instanceof ComponentFlowVersion) {
            ComponentFlowVersion flowVersion = (ComponentFlowVersion) item;
            DesignFlowLayout flowLayout = new DesignFlowLayout(configurationService, flowVersion,
                    designComponentPalette, designPropertySheet, this);
            tabs.addCloseableTab(
                    flowVersion.getComponentFlow().getName() + " " + flowVersion.getName(),
                    Icons.FLOW, flowLayout);
        }
    }

    @Override
    protected void selectionChanged(ValueChangeEvent event) {
        super.selectionChanged(event);
        boolean enabled = getSelectedFolder() != null && itemBeingEdited == null;
        newFlow.setEnabled(enabled);
        newResource.setEnabled(enabled);
        newModel.setEnabled(enabled);
    }

    @Override
    protected void folderExpanded(Folder folder) {
        super.folderExpanded(folder);
        removeAllNonFolderChildren(folder);
        addResourcesToFolder(folder);
        addComponentFlowsToFolder(folder);
    }

    protected void addNewFlow() {
        Folder folder = getSelectedFolder();
        if (folder != null) {

            Flow flow = new Flow(folder);
            flow.setName("New Flow");
            configurationService.save(flow);

            FlowVersion flowVersion = new FlowVersion(flow);
            flowVersion.setVersionName("version 1.0");
            configurationService.save(flowVersion);

            treeTable.addItem(flow);
            treeTable.setItemIcon(flow, Icons.FLOW);
            treeTable.setParent(flow, folder);
            treeTable.addItem(flowVersion);
            treeTable.setItemIcon(flowVersion, Icons.FLOW_VERSION);
            treeTable.setParent(flowVersion, flow);

            treeTable.setCollapsed(folder, false);

            startEditingItem(flow);
        }
    }

    protected void addResourcesToFolder(Folder folder) {
        List<Resource> resources = configurationService.findResourcesInFolder(folder);
        for (Resource resource : resources) {
            this.treeTable.addItem(resource);
            if (DataSourceResource.TYPE.equals(resource.getType())) {
                this.treeTable.setItemIcon(resource, Icons.DATABASE);
            } else {
                this.treeTable.setItemIcon(resource, Icons.GENERAL_RESOURCE);
            }
            this.treeTable.setChildrenAllowed(resource, false);
            this.treeTable.setParent(resource, folder);
        }

    }

    protected void addComponentFlowsToFolder(Folder folder) {
        List<Flow> flows = configurationService.findFlowsInFolder(folder);
        for (Flow flow : flows) {
            this.treeTable.addItem(flow);
            this.treeTable.setItemIcon(flow, Icons.FLOW);
            this.treeTable.setParent(flow, folder);

            List<FlowVersion> versions = flow.getFlowVersions();
            for (FlowVersion componentFlowVersion : versions) {
                this.treeTable.addItem(componentFlowVersion);
                this.treeTable.setItemCaption(componentFlowVersion,
                        componentFlowVersion.getVersionName());
                this.treeTable.setItemIcon(componentFlowVersion, Icons.FLOW_VERSION);
                this.treeTable.setParent(componentFlowVersion, flow);
                this.treeTable.setChildrenAllowed(componentFlowVersion, false);
            }
        }
    }

}
