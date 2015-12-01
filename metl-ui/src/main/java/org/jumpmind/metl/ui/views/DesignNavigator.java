/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views;

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

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.ComponentName;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.model.UserSetting;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.runtime.resource.Datasource;
import org.jumpmind.metl.core.runtime.resource.Ftp;
import org.jumpmind.metl.core.runtime.resource.Http;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.metl.core.runtime.resource.Sftp;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.EnableFocusTextField;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.views.design.EditFlowPanel;
import org.jumpmind.metl.ui.views.design.EditModelPanel;
import org.jumpmind.metl.ui.views.design.ManageProjectsPanel;
import org.jumpmind.metl.ui.views.design.PropertySheet;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
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
public class DesignNavigator extends VerticalLayout {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    TabbedPanel tabs;

    TreeTable treeTable;

    AbstractObject itemBeingEdited;

    MenuItem newMenu;

    MenuItem editMenu;

    MenuItem newFlow;

    MenuItem newModel;

    MenuItem newFileResource;
    
    MenuItem newSSHResource;

    MenuItem newDataSource;
    
    MenuItem newWebResource;
    
    MenuItem newFtpResource;

    MenuItem blank;    

    MenuItem delete;

    MenuItem closeProject;

    MenuItem exportProject;

    MenuItem search;

    FileDownloader fileDownloader;

    HorizontalLayout searchBarLayout;

    VerticalLayout openProjectsLayout;

    public DesignNavigator(ApplicationContext context, TabbedPanel tabs) {
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

        editMenu.setEnabled(false);
        newMenu.setEnabled(true);
        blank.setVisible(false);
        newFlow.setVisible(false);
        newModel.setVisible(false);
        newDataSource.setVisible(false);
        newSSHResource.setVisible(false);
        newWebResource.setVisible(false);
        newFtpResource.setVisible(false);
        newFileResource.setVisible(false);
        if (selected instanceof FolderName) {
            FolderName folder = (FolderName) selected;
            if (folder.getName().equals("Flows") || folder.getName().equals("Tests")) {
                newFlow.setVisible(true);
            } else if (folder.getName().equals("Models")) {
                newModel.setVisible(true);
            } else if (folder.getName().equals("Resources")) {
                newDataSource.setVisible(true);
                newFileResource.setVisible(true);
                newWebResource.setVisible(true);
                newSSHResource.setVisible(true);
                newFtpResource.setVisible(true);
            } else {
                blank.setVisible(true);
                newMenu.setEnabled(false);
            }
        } else {
            blank.setVisible(true);
            newMenu.setEnabled(false);

            if (selected != null && !(selected instanceof ProjectVersion)) {
                editMenu.setEnabled(true);
            }
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

        editMenu = leftMenuBar.addItem("Edit", null);

        editMenu.addItem("Open", selectedItem -> open(treeTable.getValue()));

        editMenu.addItem("Rename", selectedItem -> startEditingItem((AbstractObject) treeTable.getValue()));
        
        editMenu.addItem("Copy", selectedItem ->  copySelected());
        
        delete = editMenu.addItem("Remove", selectedItem -> handleDelete());

        MenuItem projectMenu = leftMenuBar.addItem("Project", null);
        projectMenu.addItem("Manage", selectedItem -> viewProjects());

        exportProject = projectMenu.addItem("Export", selectedItem ->  exportProject());
        closeProject = projectMenu.addItem("Close", selectedItem -> closeProject());
        blank = newMenu.addItem("", null);
        newFlow = newMenu.addItem("Flow", selectedItem -> addNewFlow());
        newModel = newMenu.addItem("Model", selectedItem -> addNewModel());
        newDataSource = newMenu.addItem("Database", selectedItem -> addNewDatabase());       
        newFtpResource = newMenu.addItem("FTP", selectedItem -> addNewFtpResource());
        newFileResource = newMenu.addItem("Local File System", selectedItem -> addNewLocalFileSystem());        
        newSSHResource = newMenu.addItem("Sftp", selectedItem -> addNewSSHFileSystem());        
        newWebResource = newMenu.addItem("Web Resource", selectedItem -> addNewHttpResource());

        MenuBar rightMenuBar = new MenuBar();
        rightMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);

        search = rightMenuBar.addItem("", Icons.SEARCH, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                search.setChecked(!search.isChecked());
                searchBarLayout.setVisible(search.isChecked());
            }
        });
        search.setVisible(false);

        layout.addComponent(leftMenuBar);
        layout.addComponent(rightMenuBar);
        layout.setExpandRatio(leftMenuBar, 1);

        return layout;
    }

    public void addProjectVersion(ProjectVersion projectVersion) {
        context.getOpenProjects().remove(projectVersion);
        context.getOpenProjects().add(projectVersion);
        Setting setting = context.getUser().findSetting(UserSetting.SETTING_CURRENT_PROJECT_ID_LIST, projectVersion.getId());
        context.getConfigurationService().save(setting);
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
                        if (table.areChildrenAllowed(event.getItemId())) {
                            Object item = event.getItemId();
                            table.setCollapsed(item, !table.isCollapsed(item));
                        }
                    }
                }
            }
        });
        table.addExpandListener(new ExpandListener() {
            @Override
            public void nodeExpand(ExpandEvent event) {
                if (event.getItemId() instanceof FolderName) {
                    table.setItemIcon(event.getItemId(), Icons.FOLDER_OPEN);
                }
            }
        });
        table.addCollapseListener(new CollapseListener() {
            @Override
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof FolderName) {
                    table.setItemIcon(event.getItemId(), Icons.FOLDER_CLOSED);
                }
            }
        });
        table.setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if ("name".equals(propertyId)) {
                    if (itemId instanceof FolderName) {
                        return "folder";
                    } else if (itemId instanceof ProjectVersion) {
                        return "project";
                    }
                }
                return null;

            }
        });

        return table;
    }

    protected Field<?> buildEditableNavigatorField(Object itemId) {
        if (itemBeingEdited != null && itemBeingEdited.equals(itemId)) {
            final EnableFocusTextField field = new EnableFocusTextField();
            field.addStyleName(ValoTheme.TEXTFIELD_SMALL);
            field.setImmediate(true);
            field.setWidth(95, Unit.PERCENTAGE);
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
            field.addValueChangeListener(event -> finishEditingItem());
            field.addBlurListener(event -> finishEditingItem());
            return field;
        } else {
            return null;
        }
    }

    protected boolean startEditingItem(AbstractObject obj) {
        if (obj.isSettingNameAllowed()) {
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
        }
    }

    protected void abortEditingItem() {
        if (itemBeingEdited != null) {
            itemBeingEdited = null;
            refresh();
            treeTable.focus();
        }
    }

    public void refresh() {
        refreshOpenProjects();
        
        setMenuItemsEnabled();

        if (treeTable.size() == 0) {
            removeComponent(treeTable);

            if (openProjectsLayout != null) {
                removeComponent(openProjectsLayout);
            }
            
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
            boolean add = true;
            Iterator<Component> i = iterator();
            while (i.hasNext()) {
                if (i.next().equals(treeTable)) {
                    add = false;
                    break;
                }
            }
            
            if (add) {
                if (openProjectsLayout != null) {
                    removeComponent(openProjectsLayout);
                }
                
                addComponent(treeTable);
                setExpandRatio(treeTable, 1);
            }
            
            treeTable.refreshRowCache();
        }
    }

    protected void refreshOpenProjects() {

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
            addFlowsToFolder(addVirtualFolder("Flows", projectVersion), projectVersion, false);
            addFlowsToFolder(addVirtualFolder("Tests", projectVersion), projectVersion, true);
            addModelsToFolder(addVirtualFolder("Models", projectVersion), projectVersion);
            addResourcesToFolder(addVirtualFolder("Resources", projectVersion), projectVersion);
            //TODO: determine if we want to show shared components here too...
            //addSharedComponentsToFolder(addVirtualFolder("Shared Components", projectVersion), projectVersion);
        }
    }

    protected FolderName addVirtualFolder(String name, ProjectVersion projectVersion) {
        String folderId = name + "-" + projectVersion.getId();
        FolderName folder = new FolderName();
        folder.setId(folderId);
        folder.setName(name);

        treeTable.addItem(folder);
        treeTable.setItemIcon(folder, Icons.FOLDER_CLOSED);
        treeTable.setItemCaption(folder, name);
        treeTable.setParent(folder, projectVersion);
        treeTable.setChildrenAllowed(folder, false);
        return folder;
    }

    protected void addResourcesToFolder(FolderName folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<ResourceName> resources = configurationService.findResourcesInProject(projectVersion
                .getId());
        for (ResourceName resource : resources) {
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(resource);
            if (Datasource.TYPE.equals(resource.getType())) {
                this.treeTable.setItemIcon(resource, Icons.DATABASE);
            } else if (Http.TYPE.equals(resource.getType())) {
                this.treeTable.setItemIcon(resource, Icons.WEB);
            } else {
                this.treeTable.setItemIcon(resource, Icons.FILE_SYSTEM);
            }
            this.treeTable.setChildrenAllowed(resource, false);
            this.treeTable.setParent(resource, folder);
        }

    }

    protected void addSharedComponentsToFolder(FolderName folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<ComponentName> components = configurationService.findSharedComponentsInProject(projectVersion
                .getId());
        for (ComponentName component : components) {
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(component);
            this.treeTable.setItemIcon(component, Icons.COMPONENT);
            this.treeTable.setParent(component, folder);
            this.treeTable.setChildrenAllowed(component, false);         
        }
    }
    
    protected void addFlowsToFolder(FolderName folder, ProjectVersion projectVersion, boolean test) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<FlowName> flows = configurationService.findFlowsInProject(projectVersion.getId(), test);
        Collections.sort(flows, new Comparator<FlowName>() {
            @Override
            public int compare(FlowName o1, FlowName o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });        
        for (FlowName flow : flows) {
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(flow);
            this.treeTable.setItemIcon(flow, Icons.FLOW);
            this.treeTable.setParent(flow, folder);
            this.treeTable.setChildrenAllowed(flow, false);
        }
    }

    protected void addModelsToFolder(FolderName folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<ModelName> models = configurationService.findModelsInProject(projectVersion.getId());
        for (ModelName model : models) {
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(model);
            this.treeTable.setItemIcon(model, Icons.MODEL);
            this.treeTable.setParent(model, folder);
            this.treeTable.setChildrenAllowed(model, false);
        }
    }

    protected void selectionChanged() {
        setMenuItemsEnabled();
    }

    public void unselectAll() {
        treeTable.setValue(null);
    }

    protected boolean isDeleteButtonEnabled(Object selected) {
        return selected instanceof FlowName || selected instanceof FlowStep
                || selected instanceof ModelName || selected instanceof ResourceName;
    }

    public void open(Object item) {
        if (item instanceof FlowName) {
            FlowName flow = (FlowName) item;
            EditFlowPanel flowLayout = new EditFlowPanel(context, flow.getId(), this, tabs);
            tabs.addCloseableTab(flow.getId(), flow.getName(), Icons.FLOW, flowLayout);
        } else if (item instanceof ModelName) {
            ModelName model = (ModelName) item;
            EditModelPanel editModel = new EditModelPanel(context, model.getId());
            tabs.addCloseableTab(model.getId(), model.getName(), Icons.MODEL, editModel);
        } else if (item instanceof ResourceName) {
            ResourceName resource = (ResourceName) item;
            PropertySheet sheet = new PropertySheet(context, tabs);            
            sheet.setSource(context.getConfigurationService().findResource(resource.getId()));
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
            ProjectVersion project = (ProjectVersion) selected;
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
            ProjectVersion projectVersion = (ProjectVersion)selected;
            context.getOpenProjects().remove(selected);
            User user = context.getUser();
            Setting setting = user.findSetting(UserSetting.SETTING_CURRENT_PROJECT_ID_LIST, projectVersion.getId());
            user.getSettings().remove(setting);
            context.getConfigurationService().delete(setting);
            refresh();
        }
    }
    
    protected void copySelected() {
        Object object = treeTable.getValue();
        if (object instanceof ModelName) {
            Model oldModel = context.getConfigurationService().findModel(((ModelName)object).getId());
            Model newModel = (Model)oldModel.copy();
            newModel.setName(newModel.getName() + " Copy");
            context.getConfigurationService().save(newModel);
            
            ModelName model = new ModelName();
            model.setName(newModel.getName());
            model.setProjectVersionId(newModel.getProjectVersionId());
            model.setId(newModel.getId());

            treeTable.addItem(model);
            treeTable.setItemIcon(model, Icons.MODEL);
            treeTable.setParent(model, treeTable.getParent(object));
            treeTable.setChildrenAllowed(model, false);
        } else if (object instanceof FlowName) {
            Flow oldFlow = context.getConfigurationService().findFlow(((FlowName)object).getId());
            Flow newFlow = (Flow)oldFlow.copy();
            newFlow.setName(newFlow.getName() + " Copy");
            context.getConfigurationService().save(newFlow);
            
            FlowName flow = new FlowName();
            flow.setName(newFlow.getName());
            flow.setProjectVersionId(newFlow.getProjectVersionId());
            flow.setId(newFlow.getId());
            flow.setTest(newFlow.isTest());

            treeTable.addItem(flow);
            treeTable.setItemIcon(flow, Icons.FLOW);
            treeTable.setParent(flow, treeTable.getParent(object));
            treeTable.setChildrenAllowed(flow, false);
            
        }
    }

    protected void handleDelete() {
        Object object = treeTable.getValue();
        if (object instanceof FlowName) {
            FlowName flow = (FlowName) object;
            ConfirmDialog.show("Delete Flow?",
                    "Are you sure you want to delete the '" + flow.getName() + "' flow?",
                    new DeleteFlowConfirmationListener(flow));
        } else if (object instanceof ResourceName) {
            ResourceName resource = (ResourceName) object;
            ConfirmDialog.show("Delete Resource?", "Are you sure you want to delete the '"
                    + resource.getName() + "' resource?", new DeleteResourceConfirmationListener(
                    resource));

        } else if (object instanceof ModelName) {
            ModelName model = (ModelName) object;
            if (!context.getConfigurationService().isModelUsed(model.getId())) {
                ConfirmDialog.show("Delete Model?",
                    "Are you sure you want to delete the '" + model.getName() + "' model?",
                    new DeleteModelConfirmationListener(model));
            } else {
                CommonUiUtils.notify("The model is currently in use.  It cannot be deleted.", Type.WARNING_MESSAGE);
            }
        }

    }

    protected FolderName findFolderWithName(String name) {
        Object value = treeTable.getValue();
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeTable.getParent(value);
        }

        if (value instanceof ProjectVersion) {
            Collection<?> children = treeTable.getChildren(value);
            for (Object object : children) {
                if (object instanceof FolderName) {
                    FolderName folder = (FolderName) object;
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
        Object selected = treeTable.getValue();
        if (selected instanceof FolderName) {
            FolderName folder = (FolderName)selected;
            treeTable.setChildrenAllowed(folder, true);

            ProjectVersion projectVersion = findProjectVersion();
            FlowName flow = new FlowName();
            flow.setProjectVersionId(projectVersion.getId());
            flow.setName("New Flow");
            flow.setTest(folder.getName().equals("Tests"));
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
        addNewResource(Datasource.TYPE, "Database", Icons.DATABASE);
    }

    protected void addNewFtpResource() {
        addNewResource(Ftp.TYPE, "FTP Site", Icons.FILE_SYSTEM);
    }
    
    protected void addNewLocalFileSystem() {
        addNewResource(LocalFile.TYPE, "Directory", Icons.FILE_SYSTEM);
    }
    
    protected void addNewSSHFileSystem() {
        addNewResource(Sftp.TYPE, "Directory", Icons.FILE_SYSTEM);
    }
    
    protected void addNewHttpResource() {
        addNewResource(Http.TYPE, "Http", Icons.WEB);
    }

    protected void addNewResource(String type, String defaultName, FontAwesome icon) {
        FolderName folder = findFolderWithName("Resources");
        if (folder != null) {
            treeTable.setChildrenAllowed(folder, true);

            ProjectVersion projectVersion = findProjectVersion();
            ResourceName resource = new ResourceName();
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
        FolderName folder = findFolderWithName("Models");
        if (folder != null) {
            treeTable.setChildrenAllowed(folder, true);

            ProjectVersion projectVersion = findProjectVersion();
            ModelName model = new ModelName();
            model.setName("New Model");
            model.setProjectVersionId(projectVersion.getId());
            context.getConfigurationService().save(model);

            treeTable.addItem(model);
            treeTable.setItemIcon(model, Icons.MODEL);
            treeTable.setParent(model, folder);
            treeTable.setChildrenAllowed(model, false);

            treeTable.setCollapsed(folder, false);

            startEditingItem(model);
        }
    }

    class DeleteFlowConfirmationListener implements IConfirmListener {

        FlowName toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteFlowConfirmationListener(FlowName toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            context.getConfigurationService().deleteFlow(context.getConfigurationService().findFlow(toDelete.getId()));
            tabs.closeTab(toDelete.getId());
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);

            return true;
        }
    }

    class DeleteResourceConfirmationListener implements IConfirmListener {

        ResourceName toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteResourceConfirmationListener(ResourceName toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            context.getConfigurationService().delete(context.getConfigurationService().findResource(toDelete.getId()));
            tabs.closeTab(toDelete.getId());
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);
            return true;
        }

    }

    class DeleteModelConfirmationListener implements IConfirmListener {

        ModelName toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteModelConfirmationListener(ModelName toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            context.getConfigurationService().delete(context.getConfigurationService().findModel(toDelete.getId()));
            tabs.closeTab(toDelete.getId());
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);

            return true;
        }
    }

}
