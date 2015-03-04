package org.jumpmind.symmetric.is.ui.views;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.ComponentFlow;
import org.jumpmind.symmetric.is.core.model.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.model.Connection;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.connection.db.DataSourceConnection;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedApplicationPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

@SuppressWarnings("serial")
public class DesignNavigator extends AbstractFolderNavigator {

    MenuItem newFlow;
    MenuItem newConnection;
    MenuItem newModel;

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

        newConnection = leftMenuBar.addItem("", Icons.GENERAL_CONNECTION, null);
        newConnection.setDescription("Add Connection");

        MenuItem newDbConnection = newConnection.addItem("Add Database", Icons.DATABASE,
                new Command() {

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                    }
                });
        newDbConnection.setDescription("Add Database Connection");

        newModel = leftMenuBar.addItem("", Icons.MODEL, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newModel.setDescription("Add Model");
    }

    @Override
    protected void openItem(Object item) {
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
        newConnection.setEnabled(enabled);
        newModel.setEnabled(enabled);
    }

    @Override
    protected void folderExpanded(Folder folder) {
        super.folderExpanded(folder);
        removeAllNonFolderChildren(folder);
        addConnectionsToFolder(folder);
        addComponentFlowsToFolder(folder);
    }

    protected void addNewFlow() {
        Folder folder = getSelectedFolder();
        if (folder != null) {

            ComponentFlow flow = new ComponentFlow(folder);
            flow.setName("New Flow");
            configurationService.save(flow);

            ComponentFlowVersion flowVersion = new ComponentFlowVersion(flow);
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

    protected void addConnectionsToFolder(Folder folder) {
        List<Connection> connections = configurationService.findConnectionsInFolder(folder);
        for (Connection connection : connections) {
            this.treeTable.addItem(connection);
            if (DataSourceConnection.TYPE.equals(connection.getType())) {
                this.treeTable.setItemIcon(connection, Icons.DATABASE);
            } else {
                this.treeTable.setItemIcon(connection, Icons.GENERAL_CONNECTION);
            }
            this.treeTable.setChildrenAllowed(connection, false);
            this.treeTable.setParent(connection, folder);
        }

    }

    protected void addComponentFlowsToFolder(Folder folder) {
        List<ComponentFlow> flows = configurationService.findComponentFlowsInFolder(folder);
        for (ComponentFlow flow : flows) {
            this.treeTable.addItem(flow);
            this.treeTable.setItemIcon(flow, Icons.FLOW);
            this.treeTable.setParent(flow, folder);

            List<ComponentFlowVersion> versions = flow.getComponentFlowVersions();
            for (ComponentFlowVersion componentFlowVersion : versions) {
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
