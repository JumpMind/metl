package org.jumpmind.symmetric.is.ui.views;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
//github.com/JumpMind/symmetric-is-all.git
import org.jumpmind.symmetric.is.core.runtime.component.DbReader;
import org.jumpmind.symmetric.is.core.runtime.component.DbWriter;
import org.jumpmind.symmetric.is.core.runtime.component.DelimitedFormatter;
import org.jumpmind.symmetric.is.core.runtime.component.EntityRouter;
import org.jumpmind.symmetric.is.core.runtime.component.FixedLengthFormatter;
import org.jumpmind.symmetric.is.core.runtime.component.MappingProcessor;
import org.jumpmind.symmetric.is.core.runtime.component.Transformer;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFileResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.EnableFocusTextField;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.is.ui.mapping.EditMappingPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditDbReaderPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditDbWriterPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditEntityRouterPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditFlowPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditFormatPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditModelPanel;
import org.jumpmind.symmetric.is.ui.views.design.EditTransformerPanel;
import org.jumpmind.symmetric.is.ui.views.design.ManageProjectsPanel;
import org.jumpmind.symmetric.is.ui.views.design.PropertySheet;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class ProjectNavigator extends VerticalLayout implements IDesignNavigator {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    TabbedPanel tabs;

    TreeTable treeTable;

    ShortcutListener treeTableEnterKeyShortcutListener;

    ShortcutListener treeTableDeleteKeyShortcutListener;

    AbstractObject itemBeingEdited;

    AbstractObject itemClicked;

    long itemClickTimeInMs;

    PropertySheet propertySheet;

    MenuItem newMenu;

    MenuItem newFlow;

    MenuItem newModel;

    MenuItem newComponent;

    MenuItem newFileResource;

    MenuItem newDataSource;

    MenuItem blank;

    MenuItem delete;

    MenuItem closeProject;

    MenuItem exportProject;

    MenuItem search;

    FileDownloader fileDownloader;

    HorizontalLayout searchBarLayout;

    VerticalLayout openProjectsLayout;

    public ProjectNavigator(ApplicationContext context, TabbedPanel tabs) {
        this.context = context;
        this.tabs = tabs;

        setSizeFull();
        addStyleName(ValoTheme.MENU_ROOT);

        addComponent(buildMenuBar());

        searchBarLayout = buildSearchBar();
        addComponent(searchBarLayout);

        treeTable = buildTreeTable();

    }

    protected HorizontalLayout buildSearchBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(new MarginInfo(false, true, true, true));
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setVisible(false);
        TextField search = new TextField();
        search.setIcon(Icons.SEARCH);
        search.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        search.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(search);
        return layout;
    }

    protected void setMenuItemsEnabled() {
        Object selected = treeTable.getValue();

        newMenu.setEnabled(true);
        blank.setVisible(false);
        newComponent.setVisible(false);
        newFlow.setVisible(false);
        newModel.setVisible(false);
        newDataSource.setVisible(false);
        newFileResource.setVisible(false);
        if (selected instanceof Folder) {
            Folder folder = (Folder) selected;
            if (folder.getName().equals("Flows")) {
                newFlow.setVisible(true);
            } else if (folder.getName().equals("Models")) {
                newModel.setVisible(true);
            } else if (folder.getName().equals("Resources")) {
                newDataSource.setVisible(true);
                newFileResource.setVisible(true);
            } else {
                blank.setVisible(true);
                newMenu.setEnabled(false);
            }
        } else {
            blank.setVisible(true);
            newMenu.setEnabled(false);
        }

        closeProject.setEnabled(selected instanceof ProjectVersion);
        exportProject.setEnabled(selected instanceof ProjectVersion);

        boolean deleteEnabled = false;
        deleteEnabled |= isDeleteButtonEnabled(treeTable.getValue());
        delete.setEnabled(deleteEnabled);

    }

    protected HorizontalLayout buildMenuBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);

        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);

        newMenu = leftMenuBar.addItem("New", null);

        MenuItem projectMenu = leftMenuBar.addItem("Project", null);
        projectMenu.addItem("Manage", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                viewProjects();
            }
        });

        exportProject = projectMenu.addItem("Export", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                exportProject();
            }
        });

        closeProject = projectMenu.addItem("Close", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                closeProject();
            }
        });

        blank = newMenu.addItem("", null);

        newFlow = newMenu.addItem("Flow", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewFlow();
            }
        });

        newModel = newMenu.addItem("Model", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewModel();
            }
        });

        newComponent = newMenu.addItem("Component", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });

        newDataSource = newMenu.addItem("Database", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewDatabase();
            }
        });

        newFileResource = newMenu.addItem("Local File System", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewFileSystem();
            }

        });

        MenuBar rightMenuBar = new MenuBar();
        rightMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);

        search = rightMenuBar.addItem("", Icons.SEARCH, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                search.setChecked(!search.isChecked());
                searchBarLayout.setVisible(search.isChecked());
            }
        });

        delete = rightMenuBar.addItem("", Icons.DELETE, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                handleDelete();
            }
        });

        layout.addComponent(leftMenuBar);
        layout.addComponent(rightMenuBar);
        layout.setExpandRatio(leftMenuBar, 1);

        return layout;
    }

    public void addProjectVersion(ProjectVersion projectVersion) {
        context.getOpenProjects().remove(projectVersion);
        context.getOpenProjects().add(projectVersion);
        refresh();
    }

    protected TreeTable buildTreeTable() {
        final TreeTable table = new TreeTable();
        table.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        table.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        table.addStyleName("noselect");
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setItemCaptionMode(ItemCaptionMode.EXPLICIT);
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setEditable(true);
        table.setContainerDataSource(new BeanItemContainer<AbstractObject>(AbstractObject.class));

        table.setTableFieldFactory(new DefaultFieldFactory() {
            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId,
                    Component uiContext) {
                return buildEditableNavigatorField(itemId);
            }
        });
        table.setVisibleColumns(new Object[] { "name" });
        table.setColumnExpandRatio("name", 1);
        treeTableDeleteKeyShortcutListener = new ShortcutListener("Delete", KeyCode.DELETE, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (delete.isEnabled()) {
                    handleDelete();
                }
            }
        };
        table.addShortcutListener(treeTableDeleteKeyShortcutListener);

        treeTableEnterKeyShortcutListener = new ShortcutListener("Enter", KeyCode.ENTER, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                open(treeTable.getValue());
            }
        };
        table.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                selectionChanged();
            }
        });
        table.addItemClickListener(new ItemClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (event.isDoubleClick()) {
                        abortEditingItem();
                        open(event.getItemId());
                        itemClicked = null;

                        if (table.areChildrenAllowed(event.getItemId())) {
                            Object item = event.getItemId();
                            table.setCollapsed(item, !table.isCollapsed(item));
                        }
                    } else {
                        if (itemClicked != null && itemClicked.equals(event.getItemId())) {
                            long timeSinceClick = System.currentTimeMillis() - itemClickTimeInMs;
                            if (timeSinceClick > 600 && timeSinceClick < 2000) {
                                startEditingItem(itemClicked);
                            } else {
                                itemClicked = null;
                            }
                        } else if (event.getItemId() instanceof AbstractObject) {
                            itemClicked = (AbstractObject) event.getItemId();
                            itemClickTimeInMs = System.currentTimeMillis();
                        }
                    }
                }
            }
        });
        table.addExpandListener(new ExpandListener() {
            @Override
            public void nodeExpand(ExpandEvent event) {
                if (event.getItemId() instanceof Folder) {
                    table.setItemIcon(event.getItemId(), Icons.FOLDER_OPEN);
                }
            }
        });
        table.addCollapseListener(new CollapseListener() {
            @Override
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof Folder) {
                    table.setItemIcon(event.getItemId(), Icons.FOLDER_CLOSED);
                }
            }
        });
        table.setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if (itemId instanceof Folder && "name".equals(propertyId)) {
                    return "folder";
                } else {
                    return null;
                }

            }
        });

        return table;
    }

    protected Field<?> buildEditableNavigatorField(Object itemId) {
        if (itemBeingEdited != null && itemBeingEdited.equals(itemId)) {
            final EnableFocusTextField field = new EnableFocusTextField();
            field.addStyleName(ValoTheme.TEXTFIELD_SMALL);
            field.setImmediate(true);
            field.addFocusListener(new FocusListener() {

                @Override
                public void focus(FocusEvent event) {
                    field.setFocusAllowed(false);
                    field.selectAll();
                    field.setFocusAllowed(true);
                }
            });
            field.focus();
            field.addShortcutListener(new ShortcutListener("Escape", KeyCode.ESCAPE, null) {

                @Override
                public void handleAction(Object sender, Object target) {
                    abortEditingItem();
                }
            });
            field.addShortcutListener(new ShortcutListener("Enter", KeyCode.ENTER, null) {

                private static final long serialVersionUID = 1L;

                @Override
                public void handleAction(Object sender, Object target) {
                    finishEditingItem();
                }
            });
            field.addBlurListener(new BlurListener() {
                @Override
                public void blur(BlurEvent event) {
                    finishEditingItem();
                }
            });
            return field;
        } else {
            return null;
        }
    }

    protected boolean startEditingItem(AbstractObject obj) {
        if (obj.isSettingNameAllowed()) {
            treeTable.removeShortcutListener(treeTableDeleteKeyShortcutListener);
            treeTable.removeShortcutListener(treeTableEnterKeyShortcutListener);
            itemBeingEdited = obj;
            treeTable.refreshRowCache();
            treeTable.setValue(null);
            return true;
        } else {
            return false;
        }
    }

    protected void finishEditingItem() {
        if (itemBeingEdited != null) {
            IConfigurationService configurationService = context.getConfigurationService();
            treeTable.addShortcutListener(treeTableDeleteKeyShortcutListener);
            Object selected = itemBeingEdited;
            Method method = null;
            try {
                method = configurationService.getClass().getMethod("save",
                        itemBeingEdited.getClass());
            } catch (NoSuchMethodException e) {
            } catch (SecurityException e) {
            }
            if (method != null) {
                try {
                    method.invoke(configurationService, itemBeingEdited);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                configurationService.save(itemBeingEdited);
            }
            itemBeingEdited = null;
            treeTable.refreshRowCache();
            treeTable.focus();
            treeTable.setValue(selected);
            selectionChanged();
        }
    }

    protected void abortEditingItem() {
        if (itemBeingEdited != null) {
            Object selected = itemBeingEdited;
            itemBeingEdited = null;
            itemClicked = null;
            refresh();
            treeTable.focus();
            treeTable.setValue(selected);
        }
    }

    @Override
    public void refresh() {
        refreshOpenProjects();

        removeComponent(treeTable);

        if (openProjectsLayout != null) {
            removeComponent(openProjectsLayout);
        }

        setMenuItemsEnabled();

        if (treeTable.size() == 0) {
            openProjectsLayout = new VerticalLayout();
            openProjectsLayout.addStyleName(ValoTheme.LAYOUT_CARD);
            openProjectsLayout.setSizeFull();
            openProjectsLayout.setMargin(true);
            Button viewProjects = new Button("Click to manage projects");
            viewProjects.addStyleName(ValoTheme.BUTTON_LINK);
            viewProjects.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    viewProjects();
                }
            });
            openProjectsLayout.addComponent(viewProjects);
            openProjectsLayout.setComponentAlignment(viewProjects, Alignment.TOP_CENTER);
            addComponent(openProjectsLayout);
            setExpandRatio(openProjectsLayout, 1);
            viewProjects();
        } else {
            addComponent(treeTable);
            setExpandRatio(treeTable, 1);
            treeTable.refreshRowCache();
        }
    }

    protected void updatePropertySheet() {
        Object obj = treeTable.getValue();
        if (propertySheet != null && obj != null) {
            if (obj instanceof FlowStep) {
                FlowStep step = (FlowStep) obj;
                if (tabs.getSelectedTab() instanceof EditFlowPanel) {
                    EditFlowPanel panel = (EditFlowPanel) tabs.getSelectedTab();
                    if (panel.getFlow().getId().equals(step.getFlowId())) {
                        panel.selected(step);
                    }
                }
            } else {
                if (tabs.getSelectedTab() instanceof EditFlowPanel) {
                    EditFlowPanel panel = (EditFlowPanel) tabs.getSelectedTab();
                    panel.selected(null);
                }
            }
        }
    }

    @Override
    public void setPropertySheet(PropertySheet propertySheet) {
        this.propertySheet = propertySheet;
        updatePropertySheet();
    }

    protected void refreshOpenProjects() {
        // add any open projects to the tree table. check cookies

        Iterator<ProjectVersion> i = context.getOpenProjects().iterator();
        while (i.hasNext()) {
            ProjectVersion projectVersion = i.next();
            context.getConfigurationService().refresh(projectVersion);
            if (projectVersion.isDeleted() || projectVersion.getProject().isDeleted()) {
                i.remove();
            }
        }

        Collections.sort(context.getOpenProjects(), new Comparator<ProjectVersion>() {
            @Override
            public int compare(ProjectVersion o1, ProjectVersion o2) {
                return o1.getProject().getName().compareTo(o2.getProject().getName());
            }
        });

        treeTable.removeAllItems();

        for (ProjectVersion projectVersion : context.getOpenProjects()) {
            treeTable.addItem(projectVersion);
            treeTable.setItemIcon(projectVersion, Icons.PROJECT);
            treeTable.setItemCaption(projectVersion, projectVersion.getProject().getName());
            treeTable.setChildrenAllowed(projectVersion, true);

            addFlowsToFolder(addVirtualFolder("Flows", projectVersion), projectVersion);
            addModelsToFolder(addVirtualFolder("Models", projectVersion), projectVersion);
            addResourcesToFolder(addVirtualFolder("Resources", projectVersion), projectVersion);
            // addComponentsToFolder(addFolder("Components", projectVersion),
            // projectVersion);
        }
    }

    protected Folder addVirtualFolder(String name, ProjectVersion projectVersion) {
        String folderId = name + "-" + projectVersion.getId();
        Folder folder = new Folder();
        folder.makeVirtual();
        folder.getProjectVersionId();
        folder.setId(folderId);
        folder.setName(name);

        treeTable.addItem(folder);
        treeTable.setItemIcon(folder, Icons.FOLDER_CLOSED);
        treeTable.setItemCaption(folder, name);
        treeTable.setParent(folder, projectVersion);
        treeTable.setChildrenAllowed(folder, false);
        return folder;
    }

    protected void addResourcesToFolder(Folder folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<Resource> resources = configurationService.findResourcesInProject(projectVersion
                .getId());
        for (Resource resource : resources) {
            this.treeTable.setChildrenAllowed(folder, true);
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

    protected void addFlowsToFolder(Folder folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<Flow> flows = configurationService.findFlowsInProject(projectVersion.getId());
        for (Flow flow : flows) {
            this.treeTable.setChildrenAllowed(folder, true);
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

    protected void addModelsToFolder(Folder folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<Model> models = configurationService.findModelsInProject(projectVersion.getId());
        for (Model model : models) {
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(model);
            this.treeTable.setItemIcon(model, Icons.MODEL);
            this.treeTable.setParent(model, folder);
            this.treeTable.setChildrenAllowed(model, false);
        }
    }

    protected void selectionChanged() {
        setMenuItemsEnabled();
        updatePropertySheet();
        treeTable.removeShortcutListener(treeTableEnterKeyShortcutListener);
        if (treeTable.getValue() != null) {
            treeTable.addShortcutListener(treeTableEnterKeyShortcutListener);
        }
    }

    public void unselectAll() {
        treeTable.setValue(null);
    }

    protected boolean isDeleteButtonEnabled(Object selected) {
        return selected instanceof Flow || selected instanceof FlowStep
                || selected instanceof Model || selected instanceof Resource;
    }

    public void open(Object item) {
        if (item instanceof FlowStep) {
            FlowStep flowStep = (FlowStep) item;
            /* TODO: these ui's need to come from component plugin
             infrastructure.  Maybe dynamically try to create edit class based on
             The component type name.  EditXxxxXxxxPanel */
            String type = flowStep.getComponent().getType();
            if (type.equals(FixedLengthFormatter.TYPE) || type.equals(DelimitedFormatter.TYPE)) {
                EditFormatPanel panel = new EditFormatPanel(context, flowStep.getComponent());
                tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel);
            } else if (type.equals(DbReader.TYPE)) {
                EditDbReaderPanel panel = new EditDbReaderPanel(context, flowStep.getComponent(),
                        propertySheet);
                tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel);
                unselectAll();
            } else if (type.equals(Transformer.TYPE)) {
                EditTransformerPanel panel = new EditTransformerPanel(context,
                        flowStep.getComponent());
                tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel);
            } else if (type.equals(DbWriter.TYPE)) {
                EditDbWriterPanel panel = new EditDbWriterPanel(context,
                        flowStep.getComponent());
                tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel); 
            } else if (type.equals(EntityRouter.TYPE)) {
                EditEntityRouterPanel panel = new EditEntityRouterPanel(context, flowStep,
                        (Flow) treeTable.getParent(flowStep));
                tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel);
            } else if (type.equals(MappingProcessor.TYPE)) {
                EditMappingPanel panel = new EditMappingPanel(context, flowStep.getComponent());
                tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel);
            } else {
                item = context.getConfigurationService().findFlow(flowStep.getFlowId());
            }
        }

        if (item instanceof Flow) {
            Flow flow = (Flow) item;
            EditFlowPanel flowLayout = new EditFlowPanel(context, flow, this, tabs);
            tabs.addCloseableTab(flow.getId(), flow.getName(), Icons.FLOW, flowLayout);
        } else if (item instanceof Model) {
            Model model = (Model) item;
            EditModelPanel editModel = new EditModelPanel(context, model);
            tabs.addCloseableTab(model.getId(), model.getName(), Icons.MODEL, editModel);
        } else if (item instanceof Resource) {
            Resource resource = (Resource) item;
            PropertySheet sheet = new PropertySheet(context);
            sheet.valueChange(resource);
            tabs.addCloseableTab(resource.getId(), resource.getName(), treeTable.getItemIcon(item),
                    sheet);
        }
    }

    protected void viewProjects() {
        tabs.addCloseableTab("projectslist", "Manage Projects", Icons.PROJECT,
                new ManageProjectsPanel(context, this));
    }

    protected void exportProject() {
        Object selected = treeTable.getValue();
        if (selected instanceof ProjectVersion) {
            ProjectVersion project = (ProjectVersion)selected;
            final String export = context.getConfigurationService().export(project);
            StreamSource ss = new StreamSource() {
                private static final long serialVersionUID = 1L;

                public InputStream getStream() {
                    try {                        
                        return new ByteArrayInputStream(export.getBytes());
                    } catch (Exception e) {
                        log.error("Failed to export configuration", e);
                        CommonUiUtils.notify("Failed to export configuration.", Type.ERROR_MESSAGE);
                        return null;
                    }

                }
            };
            String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            StreamResource resource = new StreamResource(ss, String.format("%s-config-%s.sql",
                    project.getName().toLowerCase().replaceAll(" ", "-"), datetime));
            final String KEY = "export";
            setResource(KEY, resource);
            Page.getCurrent().open(ResourceReference.create(resource, this, KEY).getURL(), null);
        }
    }

    protected void closeProject() {
        Object selected = treeTable.getValue();
        if (selected instanceof ProjectVersion) {
            context.getOpenProjects().remove(selected);
            refresh();
        }
    }

    @Override
    public void select(Object obj) {
        Object parent = obj;
        do {
            parent = treeTable.getParent(parent);
            if (parent != null) {
                treeTable.setCollapsed(parent, false);
            }
        } while (parent != null);

        treeTable.setValue(obj);
    }

    protected void handleDelete() {
        Object object = treeTable.getValue();
        if (object instanceof Flow) {
            Flow flow = (Flow) object;
            ConfirmDialog.show("Delete Flow?",
                    "Are you sure you want to delete the '" + flow.getName() + "' flow?",
                    new DeleteFlowConfirmationListener(flow));
        } else if (object instanceof Resource) {
            Resource resource = (Resource) object;
            ConfirmDialog.show("Delete Resource?", "Are you sure you want to delete the '"
                    + resource.getName() + "' resource?", new DeleteResourceConfirmationListener(
                    resource));

        } else if (object instanceof FlowStep) {
            FlowStep flowStep = (FlowStep) object;
            ConfirmDialog.show("Delete Step?",
                    "Are you sure you want to delete the '" + flowStep.getName() + "' step?",
                    new DeleteFlowStepConfirmationListener(flowStep));

        } else if (object instanceof Model) {
            Model model = (Model) object;
            ConfirmDialog.show("Delete Model?",
                    "Are you sure you want to delete the '" + model.getName() + "' model?",
                    new DeleteModelConfirmationListener(model));
        }

    }

    protected Folder findFolderWithName(String name) {
        Object value = treeTable.getValue();
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeTable.getParent(value);
        }

        if (value instanceof ProjectVersion) {
            Collection<?> children = treeTable.getChildren(value);
            for (Object object : children) {
                if (object instanceof Folder) {
                    Folder folder = (Folder) object;
                    if (folder.getName().equals(name)) {
                        return folder;
                    }
                }
            }
        }
        return null;
    }

    protected ProjectVersion findProjectVersion() {
        Object value = treeTable.getValue();
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeTable.getParent(value);
        }

        if (value instanceof ProjectVersion) {
            return ((ProjectVersion) value);
        } else {
            return null;
        }
    }

    protected void addNewFlow() {

        Folder folder = findFolderWithName("Flows");
        if (folder != null) {
            treeTable.setChildrenAllowed(folder, true);

            ProjectVersion projectVersion = findProjectVersion();
            Flow flow = new Flow();
            flow.setProjectVersionId(projectVersion.getId());
            flow.setName("New Flow");
            context.getConfigurationService().save(flow);
            treeTable.addItem(flow);
            treeTable.setItemIcon(flow, Icons.FLOW);
            treeTable.setParent(flow, folder);
            treeTable.setChildrenAllowed(flow, false);

            treeTable.setCollapsed(folder, false);
            treeTable.setCollapsed(projectVersion, false);
            treeTable.setValue(flow);

            startEditingItem(flow);
        }
    }

    protected void addNewDatabase() {
        addNewResource(DataSourceResource.TYPE, "Database", Icons.DATABASE);
    }

    protected void addNewFileSystem() {
        addNewResource(LocalFileResource.TYPE, "Directory", Icons.FILE_SYSTEM);
    }

    protected void addNewResource(String type, String defaultName, FontAwesome icon) {
        Folder folder = findFolderWithName("Resources");
        if (folder != null) {
            treeTable.setChildrenAllowed(folder, true);

            ProjectVersion projectVersion = findProjectVersion();
            Resource resource = new Resource();
            resource.setName(defaultName);
            resource.setProjectVersionId(projectVersion.getId());
            resource.setType(type);
            context.getConfigurationService().save(resource);

            treeTable.addItem(resource);
            treeTable.setItemIcon(resource, icon);
            treeTable.setParent(resource, folder);
            treeTable.setChildrenAllowed(resource, false);

            treeTable.setCollapsed(folder, false);

            startEditingItem(resource);
        }
    }

    protected void addNewModel() {
        Folder folder = findFolderWithName("Models");
        if (folder != null) {
            treeTable.setChildrenAllowed(folder, true);

            ProjectVersion projectVersion = findProjectVersion();
            Model model = new Model();
            model.setName("New Model");
            model.setProjectVersionId(projectVersion.getId());
            context.getConfigurationService().save(model);

            treeTable.addItem(model);
            treeTable.setItemIcon(model, Icons.MODEL);
            treeTable.setParent(model, folder);
            this.treeTable.setChildrenAllowed(model, false);

            treeTable.setCollapsed(folder, false);

            startEditingItem(model);
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
            context.getConfigurationService().deleteFlow(toDelete);
            tabs.closeTab(toDelete.getId());
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);

            return true;
        }
    }

    class DeleteResourceConfirmationListener implements IConfirmListener {

        Resource toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteResourceConfirmationListener(Resource toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            context.getConfigurationService().delete(toDelete);
            tabs.closeTab(toDelete.getId());
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);
            return true;
        }

    }

    class DeleteFlowStepConfirmationListener implements IConfirmListener {

        FlowStep toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteFlowStepConfirmationListener(FlowStep toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            String flowId = toDelete.getFlowId();
            Flow flow = context.getConfigurationService().findFlow(flowId);
            context.getConfigurationService().delete(flow, toDelete);
            Object parent = treeTable.getParent(toDelete);
            if (tabs.closeTab(flowId)) {
                open(flow);
            }
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);

            return true;
        }
    }

    class DeleteModelConfirmationListener implements IConfirmListener {

        Model toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteModelConfirmationListener(Model toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            context.getConfigurationService().delete(toDelete);
            tabs.closeTab(toDelete.getId());
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);

            return true;
        }
    }

}
