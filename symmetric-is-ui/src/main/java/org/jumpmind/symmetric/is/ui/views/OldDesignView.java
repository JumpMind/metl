package org.jumpmind.symmetric.is.ui.views;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.db.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.AbstractFolderNavigatorLayout;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.DesignAgentSelect;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.views.design.EditDbResourceWindow;
import org.jumpmind.symmetric.is.ui.views.design.EditFlowLayout;
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
@TopBarLink(category = Category.OLD_DESIGN, name = "Old Design", id = "oldDesign", icon = FontAwesome.SHARE_ALT, menuOrder = 10)
public class OldDesignView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    static final FontAwesome GENERAL_CONNECTION_ICON = FontAwesome.BOLT;

    static final FontAwesome FLOW_ICON = FontAwesome.SHARE_ALT;

    @Autowired
    EditDbResourceWindow editDbConnectionWindow;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IComponentFactory componentFactory;

    @Autowired
    IResourceFactory connectionFactory;

    @Autowired
    IAgentManager agentManager;

    @Autowired
    DesignAgentSelect designAgentSelect;

    protected TabbedApplicationPanel tabSheet;

    protected MainTab mainTab;

    public OldDesignView() {
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
            super(FolderType.DESIGN, OldDesignView.this.configurationService);
        }

        @Override
        protected TreeTable buildTree() {
            TreeTable table = super.buildTree();
            table.addGeneratedColumn("Artifact Type", new ColumnGenerator() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
                    if (itemId instanceof Flow) {
                        return "Flow";
                    } else if (itemId instanceof FlowVersion) {
                        return "Flow Version";
                    } else if (itemId instanceof Resource) {
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
            if (item instanceof FlowVersion) {
                EditFlowLayout editFlowLayout = new EditFlowLayout(agentManager,
                        (FlowVersion) item, configurationService, componentFactory,
                        connectionFactory, designAgentSelect);
                tabSheet.addCloseableTab(editFlowLayout.getCaption(), FLOW_ICON, editFlowLayout);
            } else if (item instanceof Resource) {
                editDbConnectionWindow.show((Resource) item, this);
            }
        }

        @Override
        public void itemUpdated(Object item) {
            if (item instanceof Resource) {
                Resource connection = (Resource) item;
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
            if (selected instanceof Flow) {
                Flow flow = (Flow) selected;
                if (!configurationService.isDeployed(flow)) {
                    enabled |= true;
                }
            }
            enabled |= super.isDeleteButtonEnabled(selected) || selected instanceof Resource;
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
            editButton.setEnabled(getSingleSelection(FlowVersion.class) != null
                    || getSingleSelection(Resource.class) != null);
        }

        @Override
        protected void deleteTreeItems(Collection<Object> objects) {
            if (objects instanceof Flow) {
                Flow flow = (Flow) objects;
                ConfirmDialog.show("Delete Flow?", "Are you sure you want to delete the "
                        + flow.getName() + " flow?", new DeleteFlowConfirmationListener(
                        flow));
            } else if (objects instanceof Resource) {
                Resource connection = (Resource) objects;
                ConfirmDialog.show("Delete Connection?", "Are you sure you want to delete the "
                        + connection.getName() + " connection?",
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
            List<Resource> connections = configurationService.findResourcesInFolder(folder);
            for (Resource connection : connections) {
                this.treeTable.addItem(connection);
                if (DataSourceResource.TYPE.equals(connection.getType())) {
                    this.treeTable.setItemIcon(connection, FontAwesome.DATABASE);
                } else {
                    this.treeTable.setItemIcon(connection, GENERAL_CONNECTION_ICON);
                }
                this.treeTable.setChildrenAllowed(connection, false);
                this.treeTable.setParent(connection, folder);
            }

        }

        protected void addComponentFlowsToFolder(Folder folder) {
            List<Flow> flows = configurationService.findFlowsInFolder(folder);
            for (Flow flow : flows) {
                this.treeTable.addItem(flow);
                this.treeTable.setItemIcon(flow, FLOW_ICON);
                this.treeTable.setParent(flow, folder);

                List<FlowVersion> versions = flow.getFlowVersions();
                for (FlowVersion componentFlowVersion : versions) {
                    this.treeTable.addItem(componentFlowVersion);
                    this.treeTable.setItemCaption(componentFlowVersion, componentFlowVersion
                            .getVersionName());
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
                editDbConnectionWindow.show(new Resource(getSelectedFolder()), MainTab.this);
            }
        }

        class NewFlowNamePromptListener implements IPromptListener {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean onOk(String content) {
                if (isNotBlank(content)) {
                    Folder folder = getSelectedFolder();

                    Flow flow = new Flow(folder);
                    flow.setName(content);

                    configurationService.save(flow);

                    FlowVersion flowVersion = new FlowVersion(flow);
                    flowVersion.setVersionName("version 1");

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

            Flow toDelete;

            private static final long serialVersionUID = 1L;

            public DeleteFlowConfirmationListener(Flow toDelete) {
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

            Resource toDelete;

            private static final long serialVersionUID = 1L;

            public DeleteConnectionConfirmationListener(Resource toDelete) {
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
