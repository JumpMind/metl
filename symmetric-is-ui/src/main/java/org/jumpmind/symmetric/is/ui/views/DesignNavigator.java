package org.jumpmind.symmetric.is.ui.views;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.connection.DataSourceConnection;
import org.jumpmind.symmetric.is.ui.support.Icons;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

@SuppressWarnings("serial")
public class DesignNavigator extends AbstractFolderNavigator {

    MenuItem newFlow;
    MenuItem newConnection;
    MenuItem newModel;

    public DesignNavigator(FolderType folderType, IConfigurationService configurationService) {
        super(folderType, configurationService);
    }

    protected void addMenuButtons(MenuBar leftMenuBar, MenuBar rightMenuBar) {
        newFlow = leftMenuBar.addItem("", Icons.FLOW, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newFlow.setDescription("New Flow");

        newConnection = leftMenuBar.addItem("", Icons.GENERAL_CONNECTION, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newConnection.setDescription("New Connection");

        newModel = leftMenuBar.addItem("", Icons.MODEL, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newModel.setDescription("New Model");
    }
    
    @Override
    protected void folderExpanded(Folder folder) {
        super.folderExpanded(folder);
        removeAllNonFolderChildren(folder);
        addConnectionsToFolder(folder);
        addComponentFlowsToFolder(folder);
    }
    
    protected void addConnectionsToFolder(Folder folder) {
        List<Connection> connections = configurationService.findConnectionsInFolder(folder);
        for (Connection connection : connections) {
            this.treeTable.addItem(connection);
            if (DataSourceConnection.TYPE.equals(connection.getData().getType())) {
                this.treeTable.setItemIcon(connection, FontAwesome.DATABASE);
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
                this.treeTable.setItemCaption(componentFlowVersion, componentFlowVersion
                        .getData().getVersionName());
                this.treeTable.setItemIcon(componentFlowVersion, FontAwesome.FILE_TEXT);
                this.treeTable.setParent(componentFlowVersion, flow);
                this.treeTable.setChildrenAllowed(componentFlowVersion, false);
            }
        }
    }
    

}
