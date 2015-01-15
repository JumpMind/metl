package org.jumpmind.symmetric.is.ui.views;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.List;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;
import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.DataSourceConnection;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.support.AbstractFolderNavigatorLayout;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.TopBarLink;
import org.jumpmind.symmetric.is.ui.views.flows.EditDbConnectionWindow;
import org.jumpmind.symmetric.is.ui.views.flows.EditFlowLayout;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.ui.common.IItemUpdatedListener;
import org.jumpmind.symmetric.ui.common.PromptDialog;
import org.jumpmind.symmetric.ui.common.PromptDialog.IPromptListener;
import org.jumpmind.symmetric.ui.common.TabbedApplicationPanel;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TreeTable;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.DESIGN, name = "Design", id = "design", icon = FontAwesome.SHARE_ALT, menuOrder = 10, useAsDefault = true)
public class DesignView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    static final FontAwesome GENERAL_CONNECTION_ICON = FontAwesome.BOLT;
    
    static final FontAwesome FLOW_ICON = FontAwesome.SHARE_ALT;

    @Autowired
    EditDbConnectionWindow editDbConnectionWindow;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IComponentFactory componentFactory;

    @Autowired
    IConnectionFactory connectionFactory;

    protected TabbedApplicationPanel tabSheet;
    
    protected MainTab mainTab;

    public DesignView() {
        setSizeFull();
    }

    @PostConstruct
    protected void init() {
        setMargin(true);
        mainTab = new MainTab();
        tabSheet = new TabbedApplicationPanel();
        tabSheet.setMainTab("Flows", FontAwesome.FOLDER_OPEN_O, mainTab);
        addComponent(tabSheet);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        mainTab.refresh();
    }

    class MainTab extends AbstractFolderNavigatorLayout implements IItemUpdatedListener {

        private static final long serialVersionUID = 1L;

        Diagram diagram;

        MenuItem addFlowButton;

        MenuItem addConnectionsButton;

        public MainTab() {
            super(FolderType.DESIGN, DesignView.this.configurationService);
        }

        @Override
        protected TreeTable buildTree() {
            TreeTable table = super.buildTree();
            table.addGeneratedColumn("Artifact Type", new ColumnGenerator() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
                    if (itemId instanceof ComponentFlow) {
                        return "Flow";
                    } else if (itemId instanceof ComponentFlowVersion) {
                        return "Flow Version";
                    } else if (itemId instanceof Connection) {
                        return "Connection";
                    } else {
                        return null;
                    }
                }
            });

            table.setColumnExpandRatio("Artifact Type", 1);

            return table;
        }

        @Override
        protected void itemClicked(Object item) {
            if (item instanceof ComponentFlowVersion) {
                EditFlowLayout editFlowLayout = new EditFlowLayout((ComponentFlowVersion) item,
                        configurationService, componentFactory, connectionFactory);
                tabSheet.addCloseableTab(editFlowLayout.getCaption(), FLOW_ICON,
                        editFlowLayout);
            } else if (item instanceof Connection) {
                editDbConnectionWindow.show((Connection) item, this);
            }
        }

        @Override
        public void itemUpdated(Object item) {
            if (item instanceof Connection) {
                Connection connection = (Connection) item;
                if (treeTable.containsId(item)) {
                    configurationService.refresh(connection);
                } else {
                    refresh();
                    treeTable.unselect(connection.getFolder());
                    expand(connection.getFolder(), item);
                }
            }
            treeTable.focus();
        }

        @Override
        protected boolean isDeleteButtonEnabled(Object selected) {
            boolean enabled = false;
            if (selected instanceof ComponentFlow) {
                ComponentFlow flow = (ComponentFlow) selected;
                if (!configurationService.isDeployed(flow)) {
                    enabled |= true;
                }
            }
            enabled |= super.isDeleteButtonEnabled(selected) || selected instanceof Connection;
            return enabled;
        }

        @Override
        protected void addToAddButton(MenuBar.MenuItem dropdown) {
            addFlowButton = dropdown.addItem("Flow", FLOW_ICON, new AddFlowCommand());
            addFlowButton.setEnabled(false);

            addConnectionsButton = dropdown.addItem("Connections", GENERAL_CONNECTION_ICON, null);
            addConnectionsButton.setEnabled(false);
            addConnectionsButton
                    .addItem("Database", FontAwesome.DATABASE, new AddDatabaseCommand());
        }

        @Override
        protected void treeSelectionChanged(ValueChangeEvent event) {
            super.treeSelectionChanged(event);
            boolean folderSelected = getSelectedFolder() != null;
            addFlowButton.setEnabled(folderSelected);
            addConnectionsButton.setEnabled(folderSelected);
            editButton.setEnabled(getSingleSelection(ComponentFlowVersion.class) != null
                    || getSingleSelection(Connection.class) != null);
        }

        @Override
        protected void deleteTreeItem(Object object) {
            if (object instanceof ComponentFlow) {
                ComponentFlow flow = (ComponentFlow) object;
                ConfirmDialog.show("Delete Flow?", "Are you sure you want to delete the "
                        + flow.getData().getName() + " flow?", new DeleteFlowConfirmationListener(
                        flow));
            } else if (object instanceof Connection) {
                Connection connection = (Connection) object;
                ConfirmDialog.show("Delete Connection?", "Are you sure you want to delete the "
                        + connection.getData().getName() + " connection?",
                        new DeleteConnectionConfirmationListener(connection));

            }
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
                    this.treeTable.setItemIcon(connection, GENERAL_CONNECTION_ICON);
                }
                this.treeTable.setChildrenAllowed(connection, false);
                this.treeTable.setParent(connection, folder);
            }

        }

        protected void addComponentFlowsToFolder(Folder folder) {
            List<ComponentFlow> flows = configurationService.findComponentFlowsInFolder(folder);
            for (ComponentFlow flow : flows) {
                this.treeTable.addItem(flow);
                this.treeTable.setItemIcon(flow, FLOW_ICON);
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

        class AddFlowCommand implements Command {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                PromptDialog.prompt("Add Flow", "Please provide a name for the new Flow",
                        new NewFlowNamePromptListener());
            }
        }

        class AddDatabaseCommand implements Command {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                editDbConnectionWindow.show(new Connection(getSelectedFolder(),
                        new ConnectionData()), MainTab.this);
            }
        }

        class NewFlowNamePromptListener implements IPromptListener {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean onOk(String content) {
                if (isNotBlank(content)) {
                    Folder folder = getSelectedFolder();

                    ComponentFlowData data = new ComponentFlowData();
                    data.setName(content);
                    data.setFolderId(folder.getData().getId());

                    ComponentFlow flow = new ComponentFlow(folder, data);

                    configurationService.save(flow);

                    ComponentFlowVersionData versionData = new ComponentFlowVersionData();
                    versionData.setVersionName("orig");
                    versionData.setComponentFlowId(data.getId());

                    ComponentFlowVersion flowVersion = new ComponentFlowVersion(flow, versionData);

                    configurationService.save(flowVersion);

                    refresh();

                    expand(folder, flow);

                    return true;
                } else {
                    return false;
                }
            }
        }

        class DeleteFlowConfirmationListener implements IConfirmListener {

            ComponentFlow toDelete;

            private static final long serialVersionUID = 1L;

            public DeleteFlowConfirmationListener(ComponentFlow toDelete) {
                this.toDelete = toDelete;
            }

            @Override
            public boolean onOk() {
                configurationService.delete(toDelete);
                refresh();
                expand(toDelete.getFolder(), toDelete.getFolder());
                return true;
            }
        }

        class DeleteConnectionConfirmationListener implements IConfirmListener {

            Connection toDelete;

            private static final long serialVersionUID = 1L;

            public DeleteConnectionConfirmationListener(Connection toDelete) {
                this.toDelete = toDelete;
            }

            @Override
            public boolean onOk() {
                configurationService.delete(toDelete);
                refresh();
                expand(toDelete.getFolder(), toDelete.getFolder());
                return true;
            }
        }

    }

}
