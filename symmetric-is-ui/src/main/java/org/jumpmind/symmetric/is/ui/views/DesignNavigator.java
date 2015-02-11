package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.support.Icons;

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

}
