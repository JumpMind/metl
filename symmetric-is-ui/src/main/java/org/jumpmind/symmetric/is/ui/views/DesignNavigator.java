package org.jumpmind.symmetric.is.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFileResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedApplicationPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditFlowPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditModelPanel;
import org.jumpmind.symmetric.is.ui.views.design.PropertySheet;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;

@SuppressWarnings("serial")
public class DesignNavigator extends AbstractFolderNavigator {

    MenuItem newFlow;

    MenuItem newResource;

    MenuItem newModel;

    MenuItem newComponent;

    ApplicationContext context;

    TabbedApplicationPanel tabs;

    PropertySheet designPropertySheet;

    public DesignNavigator(ApplicationContext context, TabbedApplicationPanel tabs) {
        super(FolderType.DESIGN, context.getConfigurationService());
        this.context = context;
        this.tabs = tabs;
        this.tabs.addCloseHandler(new CloseHandler() {
            @Override
            public void onTabClose(TabSheet tabsheet, Component tabContent) {
                treeTable.focus();
            }
        });
    }

    @Override
    protected void addMenuButtons(MenuItem newMenu, MenuBar leftMenuBar, MenuBar rightMenuBar) {

        newFlow = newMenu.addItem("Flow", Icons.FLOW, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewFlow();
            }
        });

        newModel = newMenu.addItem("Model", Icons.MODEL, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewModel();
            }
        });

        newComponent = newMenu.addItem("Component", Icons.COMPONENT, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });

        newResource = newMenu.addItem("Resource", Icons.GENERAL_RESOURCE, null);
        newResource.setDescription("Add Resource");

        newResource.addItem("Database", Icons.DATABASE, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewDatabase();
            }
        });

        newResource.addItem("Local File System", Icons.FILE_SYSTEM, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewFileSystem();
            }
        });

    }

    @Override
    protected void openItem(Object item) {
        if (item instanceof FlowStep) {
            String flowId = ((FlowStep) item).getFlowId();
            item = configurationService.findFlow(flowId);
        }

        if (item instanceof Flow) {
            Flow flow = (Flow) item;
            EditFlowPanel flowLayout = new EditFlowPanel(context, flow, this, tabs);
            tabs.addCloseableTab(flow.getId(), flow.getName() + " " + flow.getName(), Icons.FLOW,
                    flowLayout);
        } else if (item instanceof Model) {
            Model model = (Model) item;
            EditModelPanel editModel = new EditModelPanel(context, model);
            tabs.addCloseableTab(model.getId(), model.getName() + " " + model.getName(),
                    Icons.MODEL, editModel);
        } else if (item instanceof Resource) {
            Resource resource = (Resource) item;
            PropertySheet sheet = new PropertySheet(context);
            sheet.valueChange(resource);
            tabs.addCloseableTab(resource.getId(), resource.getName(), treeTable.getItemIcon(item),
                    sheet);
        }
    }

    @Override
    protected void selectionChanged(ValueChangeEvent event) {
        super.selectionChanged(event);
        boolean enabled = getSelectedFolder() != null && itemBeingEdited == null;
        newFlow.setEnabled(enabled);
        newResource.setEnabled(enabled);
        newModel.setEnabled(enabled);
        updatePropertySheet();
    }

    protected void updatePropertySheet() {
        AbstractObject obj = getSingleSelection(AbstractObject.class);
        if (obj instanceof FlowStep && designPropertySheet != null) {
            FlowStep step = (FlowStep) obj;
            if (tabs.getSelectedTab() instanceof EditFlowPanel) {
                EditFlowPanel panel = (EditFlowPanel) tabs.getSelectedTab();
                if (panel.getFlow().getId().equals(step.getFlowId())) {
                    panel.selected(step);
                }
            }
        }
    }

    public void setDesignPropertySheet(PropertySheet designPropertySheet) {
        this.designPropertySheet = designPropertySheet;
        updatePropertySheet();
    }

    @Override
    protected void folderExpanded(Folder folder) {
        super.folderExpanded(folder);
        removeAllNonFolderChildren(folder);
        addResourcesToFolder(folder);
        addFlowsToFolder(folder);
        addModelsToFolder(folder);
    }

    protected void addNewDatabase() {
        addNewResource(DataSourceResource.TYPE, "Database", Icons.DATABASE);
    }

    protected void addNewFileSystem() {
        addNewResource(LocalFileResource.TYPE, "Directory", Icons.FILE_SYSTEM);
    }

    protected void addNewResource(String type, String defaultName, FontAwesome icon) {
        Folder folder = getSelectedFolder();
        if (folder != null) {
            Resource resource = new Resource();
            resource.setName(defaultName);
            resource.setFolder(folder);
            resource.setType(type);
            configurationService.save(resource);

            treeTable.addItem(resource);
            treeTable.setItemIcon(resource, icon);
            treeTable.setParent(resource, folder);

            treeTable.setCollapsed(folder, false);

            startEditingItem(resource);
        }
    }

    @Override
    protected boolean isDeleteButtonEnabled(Object selected) {
        boolean deleteButtonEnabled = super.isDeleteButtonEnabled(selected);
        if (selected instanceof Flow) {
            Flow flow = (Flow) selected;
            if (!configurationService.isDeployed(flow)) {
                deleteButtonEnabled |= true;
            }
        } else if (selected instanceof FlowStep) {
            deleteButtonEnabled |= true;
        }
        deleteButtonEnabled |= super.isDeleteButtonEnabled(selected)
                || selected instanceof Resource;
        return deleteButtonEnabled;
    }

    @Override
    protected void deleteTreeItems(Collection<Object> objects) {
        final List<Object> treeItems = new ArrayList<Object>(objects);
        if (treeItems.size() > 0) {
            Object object = treeItems.remove(0);
            if (object instanceof Flow) {
                Flow flow = (Flow) object;
                ConfirmDialog.show("Delete Flow?",
                        "Are you sure you want to delete the " + flow.getName() + " flow?",
                        new DeleteFlowConfirmationListener(flow, treeItems));
            } else if (object instanceof Resource) {
                Resource resource = (Resource) object;
                ConfirmDialog.show("Delete Connection?", "Are you sure you want to delete the "
                        + resource.getName() + " connection?",
                        new DeleteResourceConfirmationListener(resource, treeItems));

            } else if (object instanceof FlowStep) {
                FlowStep flowStep = (FlowStep) object;
                ConfirmDialog.show("Delete Step?", "Are you sure you want to delete the "
                        + flowStep.getName() + " step?", new DeleteFlowStepConfirmationListener(
                        flowStep, treeItems));

            }

        }

    }

    protected void addNewModel() {
        Folder folder = getSelectedFolder();
        if (folder != null) {

            Model model = new Model(folder);
            model.setName("New Model");
            configurationService.save(model);

            treeTable.addItem(model);
            treeTable.setItemIcon(model, Icons.MODEL);
            treeTable.setParent(model, folder);
            this.treeTable.setChildrenAllowed(model, false);
            
            treeTable.setCollapsed(folder, false);
            
            startEditingItem(model);
        }
    }

    protected void addNewFlow() {
        Folder folder = getSelectedFolder();
        if (folder != null) {

            Flow flow = new Flow(folder);
            flow.setName("New Flow");
            configurationService.save(flow);

            treeTable.addItem(flow);
            treeTable.setItemIcon(flow, Icons.FLOW);
            treeTable.setParent(flow, folder);

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

    protected void addFlowsToFolder(Folder folder) {
        List<Flow> flows = configurationService.findFlowsInFolder(folder);
        for (Flow flow : flows) {
            this.treeTable.addItem(flow);
            this.treeTable.setItemIcon(flow, Icons.FLOW);
            this.treeTable.setParent(flow, folder);

            List<FlowStep> flowSteps = flow.getFlowSteps();

            this.treeTable.setChildrenAllowed(flow, flowSteps.size() > 0);

            for (FlowStep flowStep : flowSteps) {
                this.treeTable.addItem(flowStep);
                this.treeTable.setItemCaption(flowStep, flowStep.getName());
                this.treeTable.setItemIcon(flowStep, Icons.COMPONENT);
                this.treeTable.setParent(flowStep, flow);
                this.treeTable.setChildrenAllowed(flowStep, false);
            }
        }
    }

    protected void addModelsToFolder(Folder folder) {
        List<Model> models = configurationService.findModelsInFolder(folder);
        for (Model model : models) {
            this.treeTable.addItem(model);
            this.treeTable.setItemIcon(model, Icons.MODEL);
            this.treeTable.setParent(model, folder);
            this.treeTable.setChildrenAllowed(model, false);
        }
    }

    @Override
    protected void finishEditingItem() {
        Object beingEditted = itemBeingEdited;
        super.finishEditingItem();
        String flowVersionId = getFlowIdFor(beingEditted);
        if (flowVersionId != null) {
            if (tabs.closeTab(flowVersionId)) {
                openItem(configurationService.findFlow(flowVersionId));
            }
        }
    }

    protected String getFlowIdFor(Object obj) {
        String flowId = null;
        if (obj instanceof FlowStep) {
            flowId = ((FlowStep) obj).getFlowId();
        }

        if (obj instanceof Flow) {
            flowId = ((Flow) obj).getId();
        }

        return flowId;
    }

    class DeleteFlowConfirmationListener implements IConfirmListener {

        Flow toDelete;

        List<Object> alsoDelete;

        private static final long serialVersionUID = 1L;

        public DeleteFlowConfirmationListener(Flow toDelete, List<Object> alsoDelete) {
            this.toDelete = toDelete;
            this.alsoDelete = alsoDelete;
        }

        @Override
        public boolean onOk() {
            configurationService.deleteFlow(toDelete);
            tabs.closeTab(toDelete.getId());
            refresh();
            expand(toDelete.getFolder(), toDelete.getFolder());
            deleteTreeItems(alsoDelete);
            return true;
        }
    }

    class DeleteResourceConfirmationListener implements IConfirmListener {

        Resource toDelete;

        List<Object> alsoDelete;

        private static final long serialVersionUID = 1L;

        public DeleteResourceConfirmationListener(Resource toDelete, List<Object> alsoDelete) {
            this.toDelete = toDelete;
            this.alsoDelete = alsoDelete;
        }

        @Override
        public boolean onOk() {
            configurationService.delete(toDelete);
            refresh();
            expand(toDelete.getFolder(), toDelete.getFolder());
            deleteTreeItems(alsoDelete);
            return true;
        }

    }

    class DeleteFlowStepConfirmationListener implements IConfirmListener {

        FlowStep toDelete;

        List<Object> alsoDelete;

        private static final long serialVersionUID = 1L;

        public DeleteFlowStepConfirmationListener(FlowStep toDelete, List<Object> alsoDelete) {
            this.toDelete = toDelete;
            this.alsoDelete = alsoDelete;
        }

        @Override
        public boolean onOk() {
            String flowVersionId = toDelete.getFlowId();
            Flow flowVersion = configurationService.findFlow(flowVersionId);

            configurationService.delete(flowVersion, toDelete);
            if (tabs.closeTab(flowVersionId)) {
                openItem(configurationService.findFlow(flowVersionId));
            }

            refresh();

            deleteTreeItems(alsoDelete);

            return true;
        }
    }
}
