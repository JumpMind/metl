package org.jumpmind.symmetric.is.ui.views;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.support.AbstractFolderNavigatorLayout;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.ConfirmDialog;
import org.jumpmind.symmetric.is.ui.support.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.is.ui.support.PromptDialog;
import org.jumpmind.symmetric.is.ui.support.PromptDialog.IPromptListener;
import org.jumpmind.symmetric.is.ui.support.UiComponent;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.jumpmind.symmetric.is.ui.views.flows.EditFlowWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

@UiComponent
@Scope(value = "ui")
@ViewLink(category = Category.DESIGN, name = "Flows", id = "flows", icon = FontAwesome.SHARE_ALT, menuOrder = 10)
public class FlowView extends AbstractFolderNavigatorLayout implements View {

    private static final long serialVersionUID = 1L;

    Diagram diagram;

    MenuItem addFlowButton;
    
    Button editButton;

    @Autowired
    EditFlowWindow editFlowWindow;

    public FlowView() {
        super("Flows", FolderType.DESIGN);
        tree.addActionHandler(new ActionHandler());
    }

    @PostConstruct
    protected void init() {
        editFlowWindow.addCloseListener(new CloseListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void windowClose(CloseEvent e) {
                ComponentFlowVersion componentFlowVersion = editFlowWindow
                        .getComponentFlowVersion();
                configurationService.refresh(componentFlowVersion);
                tree.focus();
            }
        });
    }
    
    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
        tree.focus();
        Collection<?> allItems = tree.getItemIds();
        if (allItems.size() > 0) {
            tree.select(allItems.iterator().next());
        }
    }
    
    @Override
    protected void itemClicked(Object item) {
        if (item instanceof ComponentFlowVersion) {
            editFlowWindow.show((ComponentFlowVersion)item);
        }
    }
    
    @Override
    protected void addButtonsAfterAdd(HorizontalLayout buttonLayout) {
        // TODO should editButton be at the parent level?
        editButton = createButton("Edit", false, new EditButtonClickListener());
        buttonLayout.addComponent(editButton);
        buttonLayout.setComponentAlignment(editButton, Alignment.MIDDLE_LEFT);
    }

    @Override
    protected boolean isDeleteButtonEnabled(Object selected) {
        return super.isDeleteButtonEnabled(selected) || selected instanceof ComponentFlow;
    }

    @Override
    protected void addToAddButton(MenuBar.MenuItem dropdown) {
        addFlowButton = dropdown.addItem("Flow", FontAwesome.SHARE_ALT, new AddFlowCommand());
        addFlowButton.setEnabled(false);
    }

    @Override
    protected void treeSelectionChanged(ValueChangeEvent event) {
        super.treeSelectionChanged(event);
        addFlowButton.setEnabled(getSelectedFolder() != null);
        editButton.setEnabled(getSingleSelection(ComponentFlowVersion.class) != null);       
    }

    @Override
    protected void deleteTreeItem(Object object) {
        if (object instanceof ComponentFlow) {
            ComponentFlow flow = (ComponentFlow) object;
            ConfirmDialog
                    .show("Delete Flow?", "Are you sure you want to delete the "
                            + flow.getData().getName() + " flow?",
                            new DeleteFlowConfirmationListener(flow));
        }
    }

    @Override
    protected void folderExpanded(Folder folder) {
        super.folderExpanded(folder);
        removeAllNonFolderChildren(folder);
        addComponentFlowsToFolder(folder);
    }

    protected void addComponentFlowsToFolder(Folder folder) {
        List<ComponentFlow> flows = configurationService.findComponentFlowsInFolder(folder);
        for (ComponentFlow flow : flows) {
            this.tree.addItem(flow);
            this.tree.setItemCaption(flow, flow.getData().getName());
            this.tree.setItemIcon(flow, FontAwesome.SHARE_ALT);
            this.tree.setParent(flow, folder);

            List<ComponentFlowVersion> versions = flow.getComponentFlowVersions();
            for (ComponentFlowVersion componentFlowVersion : versions) {
                this.tree.addItem(componentFlowVersion);
                this.tree.setItemCaption(componentFlowVersion, componentFlowVersion.getData()
                        .getVersionName());
                this.tree.setItemIcon(componentFlowVersion, FontAwesome.FILE_TEXT);
                this.tree.setParent(componentFlowVersion, flow);
                this.tree.setChildrenAllowed(componentFlowVersion, false);
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

    class NewFlowNamePromptListener implements IPromptListener {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean onOk(String content) {
            if (isNotBlank(content)) {
                Folder folder = getSelectedFolder();

                ComponentFlowData data = new ComponentFlowData();
                data.setName(content);
                data.setFolderId(folder.getData().getId());

                ComponentFlow flow = new ComponentFlow(data);

                configurationService.save(flow);

                ComponentFlowVersionData versionData = new ComponentFlowVersionData();
                versionData.setVersionName("orig");
                versionData.setComponentFlowId(data.getId());

                ComponentFlowVersion flowVersion = new ComponentFlowVersion(flow, versionData);

                configurationService.save(flowVersion);

                refresh();

                while (folder != null) {
                    tree.setCollapsed(folder, false);
                    folder = folder.getParent();
                }

                Set<Object> selected = new HashSet<Object>();
                selected.add(flow);
                tree.setValue(selected);
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
            configurationService.deleteComponentFlow(toDelete);
            refresh();
            return true;
        }
    }

    class ActionHandler implements Handler {

        private static final String ACTION_OPEN_FLOW = "Open Flow";

        private static final long serialVersionUID = 1L;

        @Override
        public Action[] getActions(Object target, Object sender) {
            if (target instanceof ComponentFlowVersion) {
                return new Action[] { new Action(ACTION_OPEN_FLOW) };
            } else {
                return null;
            }
        }

        @Override
        public void handleAction(Action action, Object sender, Object target) {
            if (action.getCaption().equals(ACTION_OPEN_FLOW)) {
                editFlowWindow.show((ComponentFlowVersion) target);
            }
        }
    }
    
    class EditButtonClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;


        @Override
        public void buttonClick(ClickEvent event) {
            ComponentFlowVersion flowVersion = getSingleSelection(ComponentFlowVersion.class);
            if (flowVersion != null) {
                itemClicked(flowVersion);
            }
        }
    }

}
