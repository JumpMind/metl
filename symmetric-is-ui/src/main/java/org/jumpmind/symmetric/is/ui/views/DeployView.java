package org.jumpmind.symmetric.is.ui.views;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.FlowVersionSummary;
import org.jumpmind.symmetric.is.core.model.DeploymentStatus;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.is.ui.common.AbstractFolderNavigatorLayout;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
import org.jumpmind.symmetric.is.ui.views.manage.EditAgentDeploymentsWindow;
import org.jumpmind.symmetric.is.ui.views.manage.EditAgentWindow;
import org.jumpmind.symmetric.is.ui.views.manage.SelectComponentFlowVersionWindow;
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
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.DEPLOY, name = "Deploy", id = "deploy", icon = FontAwesome.GEARS, menuOrder = 10)
public class DeployView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    static final FontAwesome DEPLOYMENT_ICON = FontAwesome.CUBE;

    protected MainTab mainTab;

    @Autowired
    EditAgentDeploymentsWindow editAgentDeploymentsWindow;

    @Autowired
    EditAgentWindow editAgentWindow;

    @Autowired
    SelectComponentFlowVersionWindow selectComponentFlowVersionWindow;

    @Autowired
    IAgentManager agentManager;

    @Autowired
    IConfigurationService configurationService;

    protected TabbedApplicationPanel tabSheet;

    public DeployView() {
        setSizeFull();
    }

    @PostConstruct
    protected void init() {
        setMargin(true);
        mainTab = new MainTab();
        tabSheet = new TabbedApplicationPanel();
        tabSheet.setMainTab("Agents", FontAwesome.FOLDER_OPEN_O, mainTab);
        addComponent(tabSheet);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        mainTab.refresh();
    }

    public class MainTab extends AbstractFolderNavigatorLayout implements IItemUpdatedListener {

        private static final long serialVersionUID = 1L;

        MenuItem addAgentButton;

        MenuItem addDeploymentButton;

        public MainTab() {
            super(FolderType.RUNTIME, DeployView.this.configurationService);
        }

        @PostConstruct
        protected void init() {
            editAgentWindow.addCloseListener(new CloseListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public void windowClose(CloseEvent e) {
                    Agent agent = editAgentWindow.getAgent();
                    configurationService.refresh(agent);
                    treeTable.focus();
                }
            });
        }

        @Override
        protected TreeTable buildTree() {
            TreeTable tree = super.buildTree();
            tree.addGeneratedColumn("Local", new ColumnGenerator() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
                    if (itemId instanceof Agent) {
                        Agent agent = (Agent) itemId;
                        if (agentManager.isAgentLocal(agent)) {
                            Label label = new Label(FontAwesome.CHECK.getHtml(), ContentMode.HTML);
                            label.addStyleName("centerAligned");
                            return label;
                        }
                    }
                    return null;
                }
            });
            tree.setColumnWidth("Local", 50);

            tree.addGeneratedColumn("Status", new ColumnGenerator() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
                    if (itemId instanceof Agent) {
                        Agent agent = (Agent) itemId;
                        List<AgentDeployment> deployments = agent.getAgentDeployments();
                        for (AgentDeployment deployment : deployments) {
                            if (deployment.getDeploymentStatus() == DeploymentStatus.ERROR) {
                                return DeploymentStatus.ERROR.toString();
                            }
                        }
                        return agent.getAgentStatus().toString();
                    } else {
                        return null;
                    }
                }
            });
            tree.setColumnWidth("Status", 75);

            tree.addGeneratedColumn("Host", new ColumnGenerator() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
                    if (itemId instanceof Agent) {
                        Agent agent = (Agent) itemId;
                        return agent.getHost();
                    } else {
                        return null;
                    }
                }
            });
            tree.setColumnExpandRatio("Host", 1);

            tree.addGeneratedColumn("Deployments", new ColumnGenerator() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
                    if (itemId instanceof Agent) {
                        Agent agent = (Agent) itemId;
                        return agent.getAgentDeployments().size();
                    } else {
                        return null;
                    }
                }
            });

            return tree;
        }

        @Override
        protected void itemClicked(Object item) {
            if (item instanceof Agent) {
                editAgentWindow.show((Agent) item, this);
            }
        }

        @Override
        public void itemUpdated(Object item) {
            if (item instanceof Agent) {
                Agent agent = (Agent) item;
                agentManager.refresh(agent);
                refresh();
                expand(agent.getFolder(), item);
            }
            treeTable.focus();
        }

        @Override
        protected boolean isDeleteButtonEnabled(Object selected) {
            boolean enabled = super.isDeleteButtonEnabled(selected);
            if (selected instanceof Agent) {
                Agent agent = (Agent) selected;
                enabled = agent.getAgentDeployments().size() == 0;
            } else if (selected instanceof AgentDeployment) {
                enabled = true;
            }
            return enabled;
        }

        @Override
        protected void addToAddButton(MenuBar.MenuItem dropdown) {
            addAgentButton = dropdown.addItem("Agent", FontAwesome.GEAR, new AddAgentCommand());
            addAgentButton.setEnabled(false);

            addDeploymentButton = dropdown.addItem("Deployment", DEPLOYMENT_ICON,
                    new AddDeploymentCommand());
            addDeploymentButton.setEnabled(false);

        }

        @Override
        protected void treeSelectionChanged(ValueChangeEvent event) {
            super.treeSelectionChanged(event);
            boolean folderSelected = getSelectedFolder() != null;
            addAgentButton.setEnabled(folderSelected);
            boolean agentSelected = getSingleSelection(Agent.class) != null;
            editButton.setEnabled(agentSelected);
            addDeploymentButton.setEnabled(agentSelected);
        }

        @Override
        protected void deleteTreeItems(final Collection<Object> objects) {
            if (objects != null && objects.size() > 0) {
                Iterator<Object> i = objects.iterator();
                Object object = i.next();
                i.remove();
                if (object instanceof Agent) {
                    Agent agent = (Agent) object;
                    ConfirmDialog.show("Delete Agent?", "Are you sure you want to delete the "
                            + agent.getName() + " agent?",
                            new DeleteAgentConfirmationListener(agent, objects));
                } else if (object instanceof AgentDeployment) {
                    AgentDeployment deployment = (AgentDeployment) object;
                    ConfirmDialog.show("Delete Deployment?", "Are you sure you want to delete the "
                            + deployment + " deployment?", new DeleteDeploymentConfirmationListener(
                            deployment, objects));

                }
            }
        }

        @Override
        protected void folderExpanded(Folder folder) {
            super.folderExpanded(folder);
            removeAllNonFolderChildren(folder);
            addAgentsToFolder(folder);
        }

        protected void addAgentsToFolder(Folder folder) {
            List<Agent> agents = configurationService.findAgentsInFolder(folder);
            for (Agent agent : agents) {
                this.treeTable.addItem(agent);
                this.treeTable.setItemIcon(agent, FontAwesome.GEAR);
                this.treeTable.setChildrenAllowed(agent, agent.getAgentDeployments().size() > 0);
                this.treeTable.setParent(agent, folder);

                List<AgentDeployment> deployments = agent.getAgentDeployments();
                for (AgentDeployment agentDeployment : deployments) {
                    this.treeTable.addItem(agentDeployment);
                    this.treeTable.setItemIcon(agentDeployment, DEPLOYMENT_ICON);
                    this.treeTable.setParent(agentDeployment, agent);
                    this.treeTable.setChildrenAllowed(agentDeployment, false);
                }
            }

        }

        class EditAgentDeploymentsClickListener implements ClickListener {
            private static final long serialVersionUID = 1L;

            Agent agent;

            public EditAgentDeploymentsClickListener(Agent agent) {
                this.agent = agent;
            }

            @Override
            public void buttonClick(ClickEvent event) {
                editAgentDeploymentsWindow.show(agent, MainTab.this);
            }
        }

        class AddAgentCommand implements Command {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                PromptDialog.prompt("Add Agent", "Please provide a name for the new Agent",
                        new NewAgentNamePromptListener());
            }
        }

        class AddDeploymentCommand implements Command, IItemUpdatedListener {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                Agent agent = getSingleSelection(Agent.class);
                if (agent != null) {
                    selectComponentFlowVersionWindow.show(agent, this);
                }
            }

            @Override
            public void itemUpdated(Object item) {
                Agent agent = getSingleSelection(Agent.class);
                @SuppressWarnings("unchecked")
                Set<FlowVersionSummary> selectedFlows = (Set<FlowVersionSummary>) item;
                for (FlowVersionSummary componentFlowVersionSummary : selectedFlows) {
                    FlowVersion componentFlowVersion = new FlowVersion();
                    componentFlowVersion.setId(componentFlowVersionSummary.getId());
                    configurationService.refresh(componentFlowVersion);
                    agentManager.deploy(agent.getId(), componentFlowVersion);
                    refresh();
                    expand(agent.getFolder(), agent);
                }
            }
        }

        class NewAgentNamePromptListener implements IPromptListener {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean onOk(String content) {
                if (isNotBlank(content)) {
                    Folder folder = getSelectedFolder();

                    Agent agent = new Agent(folder);
                    agent.setName(content);

                    configurationService.save(agent);

                    refresh();

                    expand(folder, agent);

                    return true;
                } else {
                    return false;
                }
            }
        }

        class DeleteAgentConfirmationListener implements IConfirmListener {

            Agent toDelete;

            Collection<Object> alsoDelete;

            private static final long serialVersionUID = 1L;

            public DeleteAgentConfirmationListener(Agent toDelete, Collection<Object> alsoDelete) {
                this.toDelete = toDelete;
                this.alsoDelete = alsoDelete;
            }

            @Override
            public boolean onOk() {
                agentManager.remove(toDelete);
                configurationService.delete(toDelete);
                refresh();
                expand(toDelete.getFolder(), toDelete.getFolder());
                deleteTreeItems(alsoDelete);
                return true;
            }
        }

        class DeleteDeploymentConfirmationListener implements IConfirmListener {

            AgentDeployment toDelete;

            Collection<Object> alsoDelete;

            private static final long serialVersionUID = 1L;

            public DeleteDeploymentConfirmationListener(AgentDeployment toDelete,
                    Collection<Object> alsoDelete) {
                this.toDelete = toDelete;
                this.alsoDelete = alsoDelete;
            }

            @Override
            public boolean onOk() {
                agentManager.undeploy(toDelete);
                configurationService.delete(toDelete);
                Agent agent = findObjectInTreeWithId(toDelete.getAgentId());
                refresh();
                expand(agent.getFolder(), agent);
                deleteTreeItems(alsoDelete);
                return true;
            }
        }

    }

}
