package org.jumpmind.symmetric.is.ui.views;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.List;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.AgentData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.ConfirmDialog;
import org.jumpmind.symmetric.is.ui.support.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.is.ui.support.PromptDialog;
import org.jumpmind.symmetric.is.ui.support.PromptDialog.IPromptListener;
import org.jumpmind.symmetric.is.ui.support.UiComponent;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.jumpmind.symmetric.is.ui.views.agents.EditAgentWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

@UiComponent
@Scope(value = "ui")
@ViewLink(category = Category.RUNTIME, name = "Agents", id = "agents", icon = FontAwesome.GEARS, menuOrder = 10)
public class AgentsView extends AbstractFolderView {

    private static final long serialVersionUID = 1L;

    @Autowired
    EditAgentWindow editAgentWindow;

    MenuItem addAgentButton;

    public AgentsView() {
        super("Agents", FolderType.RUNTIME);
    }

    @PostConstruct
    protected void init() {
        editAgentWindow.addCloseListener(new CloseListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void windowClose(CloseEvent e) {
                Agent agent = editAgentWindow.getAgent();
                configurationService.refresh(agent);
                tree.focus();
            }
        });
    }

    @Override
    protected void itemClicked(Object item) {
        if (item instanceof Agent) {
            editAgentWindow.show((Agent) item, this);
        }
    }

    @Override
    public void itemSaved(Object item) {
        if (item instanceof Agent) {
            Agent agent = (Agent) item;
            refresh();
            expand(agent.getFolder(), item);
        }
        tree.focus();
    }

    @Override
    protected void addButtonsAfterAdd(HorizontalLayout buttonLayout) {
    }

    @Override
    protected boolean isDeleteButtonEnabled(Object selected) {
        return super.isDeleteButtonEnabled(selected) || selected instanceof Agent;
    }

    @Override
    protected void addToAddButton(MenuBar.MenuItem dropdown) {
        addAgentButton = dropdown.addItem("Agent", FontAwesome.GEAR, new AddAgentCommand());
        addAgentButton.setEnabled(false);
    }

    @Override
    protected void treeSelectionChanged(ValueChangeEvent event) {
        super.treeSelectionChanged(event);
        boolean folderSelected = getSelectedFolder() != null;
        addAgentButton.setEnabled(folderSelected);
        editButton.setEnabled(getSingleSelection(Agent.class) != null);
    }

    @Override
    protected void deleteTreeItem(Object object) {
        if (object instanceof Agent) {
            Agent agent = (Agent) object;
            ConfirmDialog.show("Delete Agent?", "Are you sure you want to delete the "
                    + agent.getData().getName() + " agent?", new DeleteAgentConfirmationListener(
                    agent));
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
            this.tree.addItem(agent);
            this.tree.setItemIcon(agent, FontAwesome.GEAR);
            this.tree.setChildrenAllowed(agent, false);
            this.tree.setParent(agent, folder);
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

    class NewAgentNamePromptListener implements IPromptListener {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean onOk(String content) {
            if (isNotBlank(content)) {
                Folder folder = getSelectedFolder();

                AgentData data = new AgentData();
                data.setName(content);
                data.setFolderId(folder.getData().getId());

                Agent agent = new Agent(folder, data);

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

        private static final long serialVersionUID = 1L;

        public DeleteAgentConfirmationListener(Agent toDelete) {
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
