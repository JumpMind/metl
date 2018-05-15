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
package org.jumpmind.metl.ui.views.design;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.metl.core.model.AbstractName;
import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.UserSetting;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.CutCopyPasteManager;
import org.jumpmind.metl.ui.common.EnableFocusTextField;
import org.jumpmind.metl.ui.common.ExportDialog;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.ImportDialog;
import org.jumpmind.metl.ui.common.ImportDialog.IImportListener;
import org.jumpmind.metl.ui.common.SelectProjectVersionDialog;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.views.design.menu.DesignMenuBar;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.ConfirmDialog.IConfirmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class DesignNavigator extends VerticalLayout {

    public static final String LABEL_DEPENDENCIES = "Dependencies";

    public static final String LABEL_FLOWS = "Flows";

    public static final String LABEL_TESTS = "Tests";

    public static final String LABEL_MODELS = "Models";

    public static final String LABEL_RESOURCES = "Resources";

    final Logger log = LoggerFactory.getLogger(getClass());

    boolean rebuilding = false;

    ApplicationContext context;

    TabbedPanel tabs;

    TreeTable treeTable;

    AbstractNamedObject itemBeingEdited;

    FileDownloader fileDownloader;

    DesignMenuBar menuBar;

    IConfigurationService configurationService;

    CutCopyPasteManager cutCopyPasteManager;
    
    BeanItemContainer<AbstractNamedObject> treeTblContainer = new BeanItemContainer<AbstractNamedObject>(AbstractNamedObject.class);
    
    String tagFilterText=null;

    public DesignNavigator(ApplicationContext context, TabbedPanel tabs) {
        this.context = context;
        this.tabs = tabs;
        this.configurationService = context.getConfigurationService();
        this.cutCopyPasteManager = new CutCopyPasteManager(context);

        setSizeFull();
        addStyleName(ValoTheme.MENU_ROOT);
        buildTreeTable();
        HorizontalLayout hLayout = new HorizontalLayout();
        menuBar = new DesignMenuBar(this, treeTable);
        hLayout.addComponent(menuBar);
        hLayout.addComponent(buildFilterField());
        hLayout.setSpacing(true);
        addComponent(hLayout);
    }

    public void addNewProject() {
        Project project = new Project();
        project.setName("New Project");
        ProjectVersion version = new ProjectVersion();
        version.setVersionType(ProjectVersion.VersionType.MASTER.toString());
        version.setVersionLabel("master");
        version.setProject(project);
        project.getProjectVersions().add(version);
        configurationService.save(project);
        configurationService.save(version);
        context.getDefinitionFactory().refresh(version.getId());
        refreshProjects();
        startEditingItem(project);
    }

    protected TextField buildFilterField() {
        TextField filterField = new TextField();
        filterField.setIcon(Icons.SEARCH);
        filterField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filterField.setInputPrompt("Tag Filter");
        filterField.setImmediate(true);
        filterField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        filterField.setTextChangeTimeout(200);
        filterField.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                if (!event.getText().isEmpty()) {
                    tagFilterText=event.getText();
                } else {
                    tagFilterText = null;
                }
                refreshProjects();
            }
        });        
        return filterField;
    }

    protected TreeTable buildTreeTable() {
        treeTable = new TreeTable();
        treeTable.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        treeTable.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        treeTable.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        treeTable.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        treeTable.addStyleName("noselect");
        treeTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        treeTable.setItemCaptionMode(ItemCaptionMode.EXPLICIT);
        treeTable.setSizeFull();
        treeTable.setCacheRate(100);
        treeTable.setPageLength(100);
        treeTable.setImmediate(true);
        treeTable.setSelectable(true);
        treeTable.setEditable(true);
        treeTable.setContainerDataSource(treeTblContainer);
        treeTable.setTableFieldFactory(new DefaultFieldFactory() {
            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {
                return buildEditableNavigatorField(itemId);
            }
        });
        treeTable.setVisibleColumns(new Object[] { "name" });
        treeTable.setColumnExpandRatio("name", 1);
        treeTable.addItemClickListener(event -> {
            if (event.getButton() == MouseButton.LEFT) {
                if (event.isDoubleClick()) {
                    abortEditingItem();
                    open(event.getItemId());
                    if (treeTable.areChildrenAllowed(event.getItemId())) {
                        Object item = event.getItemId();
                        treeTable.setCollapsed(item, !treeTable.isCollapsed(item));
                    }
                }
            }
        });
        treeTable.addExpandListener(e -> {
            // deselect any selected rows when they expand or collapse?
            AbstractObject object = (AbstractObject) treeTable.getValue();
            treeTable.unselect(object);
            if (e.getItemId() instanceof FolderName) {
                treeTable.setItemIcon(e.getItemId(), Icons.FOLDER_OPEN);
            } else if (e.getItemId() instanceof Project && !treeTable.hasChildren(e.getItemId())) {
                addProjectVersions((Project) e.getItemId());
            }

            saveExpandedList();
        });
        treeTable.addCollapseListener(event -> {
            if (event.getItemId() instanceof FolderName) {
                treeTable.setItemIcon(event.getItemId(), Icons.FOLDER_CLOSED);
            }
            saveExpandedList();
        });
        treeTable.setCellStyleGenerator((Table source, Object itemId, Object propertyId) -> {
            if ("name".equals(propertyId)) {
                if (itemId instanceof FolderName) {
                    return "folder";
                } else if (itemId instanceof Project) {
                    return "project";
                } else if (itemId instanceof ProjectVersion) {
                    ProjectVersion version = (ProjectVersion) itemId;
                    return version.locked() ? "project-version-read-only" : "project-version";
                } else {
                    ProjectVersion version = findProjectVersion(itemId);
                    if (version != null) {
                        return version.locked() ? "project-version-read-only" : null;
                    }
                }
            }
            return null;
        });
        treeTable.addValueChangeListener(e -> selectionChanged());

        return treeTable;
    }

    protected void saveExpandedList() {
        if (!rebuilding) {
            StringBuilder ids = new StringBuilder();
            Collection<?> itemIds = treeTable.getItemIds();
            for (Object itemId : itemIds) {
                if (!treeTable.isCollapsed(itemId)) {
                    if (itemId instanceof FolderName) {
                        Collection<?> children = treeTable.getChildren(itemId);
                        if (children.size() > 0) {
                            itemId = children.iterator().next();
                        }
                    }

                    if (!(itemId instanceof FolderName)) {
                        ids.append(itemId.getClass().getSimpleName()).append(":").append(((AbstractObject) itemId).getId()).append(";");
                    }
                }
            }

            Setting setting = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_EXPANDED_IDS);
            setting.setValue(ids.toString());
            configurationService.save(setting);
        }
    }

    protected void selectionChanged() {
        AbstractObject object = (AbstractObject) treeTable.getValue();
        if (object != null) {
            Setting setting = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_SELECTION_ID);
            setting.setValue(object.getId());
            configurationService.save(setting);

            if (!(object instanceof Project)) {
                String projectId = findProjectVersion().getProjectId();
                setting = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_SELECTED_PROJECT_ID);
                setting.setValue(projectId);
                configurationService.save(setting);
            }
        }
    }

    protected Field<?> buildEditableNavigatorField(Object itemId) {
        if (itemBeingEdited != null && itemBeingEdited.equals(itemId)) {
            final EnableFocusTextField field = new EnableFocusTextField();
            field.addStyleName(ValoTheme.TEXTFIELD_SMALL);
            field.setImmediate(true);
            field.setWidth(95, Unit.PERCENTAGE);
            field.addFocusListener(e -> {
                field.setFocusAllowed(false);
                field.selectAll();
                field.setFocusAllowed(true);
            });
            field.focus();
            field.addShortcutListener(new ShortcutListener("Escape", KeyCode.ESCAPE, null) {

                @Override
                public void handleAction(Object sender, Object target) {
                    abortEditingItem();
                }
            });
            field.addValueChangeListener(event -> finishEditingItem((String) event.getProperty().getValue()));
            field.addBlurListener(event -> abortEditingItem());
            return field;
        } else {
            return null;
        }
    }

    public boolean startEditingItem(AbstractNamedObject obj) {
        if (obj.isSettingNameAllowed()) {
            itemBeingEdited = obj;
            treeTable.refreshRowCache();
            treeTable.setValue(null);
            return true;
        } else {
            return false;
        }
    }

    protected void finishEditingItem(String value) {
        if (itemBeingEdited != null && isNotBlank(value)) {
            itemBeingEdited.setName(value);
            Object selected = itemBeingEdited;
            Method method = null;
            try {
                method = configurationService.getClass().getMethod("save", itemBeingEdited.getClass());
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
            treeTable.setValue(selected);
            refreshProjects();
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
        refreshProjects();

        menuBar.refresh();

        boolean add = true;
        Iterator<Component> i = iterator();
        while (i.hasNext()) {
            if (i.next().equals(treeTable)) {
                add = false;
                break;
            }
        }

        if (add) {
            addComponent(treeTable);
            setExpandRatio(treeTable, 1);
        }
        treeTable.refreshRowCache();
    }

    protected void addProjectVersions(Project project) {
        List<ProjectVersion> versions = project.getProjectVersions();
        for (ProjectVersion projectVersion : versions) {
            treeTable.addItem(projectVersion);
            if (projectVersion.locked()) {
                treeTable.setItemIcon(projectVersion, FontAwesome.LOCK);
            } else {
                treeTable.setItemIcon(projectVersion, Icons.PROJECT_VERSION);
            }

            treeTable.setChildrenAllowed(projectVersion, true);
            treeTable.setParent(projectVersion, project);
            addFlowsToFolder(LABEL_FLOWS, projectVersion, false);
            addFlowsToFolder(LABEL_TESTS, projectVersion, true);
            addModelsToFolder(LABEL_MODELS, projectVersion);
            addResourcesToFolder(LABEL_RESOURCES, projectVersion);
            addDependenciesToFolder(LABEL_DEPENDENCIES, projectVersion);
        }
    }

    protected void collapseAll(Object itemId) {
        treeTable.setCollapsed(itemId, true);
        Collection<?> itemIds = treeTable.getChildren(itemId);
        if (itemIds != null) {
            for (Object object : itemIds) {
                treeTable.setCollapsed(object, true);
            }
        }
    }

    protected void refreshProjects() {
        rebuilding = true;
        try {
            long ts = System.currentTimeMillis();
            Object selected = treeTable.getValue();
            List<Project> projects;
            if (tagFilterText == null) {
                projects = configurationService.findProjects();
            } else {
                projects = configurationService.findProjectsWithTagLike(tagFilterText);
            }
            Collection<?> itemIds = treeTable.getItemIds();
            for (Object itemId : itemIds) {
                collapseAll(itemId);
            }
            treeTable.removeAllItems();
            for (Project project : projects) {
                List<ProjectVersion> versions = project.getProjectVersions();
                treeTable.addItem(project);
                treeTable.setItemIcon(project, Icons.PROJECT);
                treeTable.setChildrenAllowed(project, versions.size() > 0);
            }

            if (selected == null) {
                String selectedId = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_SELECTION_ID).getValue();
                if (isNotBlank(selectedId)) {
                    String projectId = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_SELECTED_PROJECT_ID).getValue();
                    if (isNotBlank(projectId)) {
                        Collection<?> items = treeTable.getItemIds();
                        for (Object object : items) {
                            if (object instanceof Project && ((Project) object).getId().equals(projectId)
                                    && !((Project) object).isDeleted()) {
                                addProjectVersions((Project) object);
                                selected = findChild(selectedId, object);
                                break;
                            }
                        }
                    }
                }
            }
            Setting setting = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_EXPANDED_IDS);
            String ids = setting.getValue();
            if (isNotBlank(ids)) {
                String[] idList = ids.split(";");
                for (String typeIdString : idList) {
                    String[] typeAndId = typeIdString.split(":");
                    String type = typeAndId[0];
                    String id = typeAndId[1];
                    AbstractObject object = null;
                    if (type.equals(FlowName.class.getSimpleName())) {
                        object = new FlowName();
                        object.setId(id);
                    } else if (type.equals(ModelName.class.getSimpleName())) {
                        object = new ModelName();
                        object.setId(id);
                    } else if (type.equals(ResourceName.class.getSimpleName())) {
                        object = new ResourceName();
                        object.setId(id);
                    } else if (type.equals(ProjectVersion.class.getSimpleName())) {
                        object = new ProjectVersion();
                        object.setId(id);
                    } else if (type.equals(ProjectVersionDepends.class.getSimpleName())) {
                        object = new ProjectVersionDepends();
                        object.setId(id);
                    }
                    
                    if (object != null) {
                        expand(object);
                    }
                }
            }
            
            if (selected != null) {
                expand(selected);
                treeTable.setValue(selected);
            }
            log.debug("It took {}ms to refresh projects in the design view", (System.currentTimeMillis() - ts));
        } finally {
            rebuilding = false;
        }
    }

    protected Object findChild(String id, Object parent) {
        Collection<?> items = treeTable.getChildren(parent);
        if (items != null) {
            for (Object object : items) {
                if (id.equals(((AbstractObject) object).getId())) {
                    return object;
                } else {
                    Object obj = findChild(id, object);
                    if (obj != null) {
                        return obj;
                    }
                }
            }
        }
        return null;

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

    protected void addResourcesToFolder(String folderName, ProjectVersion projectVersion) {
        FolderName folder = null;
        List<ResourceName> resources = context.getUiCache().findResourcesInProject(projectVersion.getId());
        for (ResourceName resource : resources) {
            if (folder == null) {
                folder = addVirtualFolder(folderName, projectVersion);
            }
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(resource);
            if ("Database".equals(resource.getType())) {
                this.treeTable.setItemIcon(resource, Icons.DATABASE);
            } else if ("Http".equals(resource.getType())) {
                this.treeTable.setItemIcon(resource, Icons.WEB);
            } else if ("MailSession".equals(resource.getType())) {
                this.treeTable.setItemIcon(resource, Icons.EMAIL);
            } else if (resource.getType().contains("JMS")) {
                this.treeTable.setItemIcon(resource, Icons.QUEUE);                
            } else {
                this.treeTable.setItemIcon(resource, Icons.FILE_SYSTEM);
            }
            this.treeTable.setChildrenAllowed(resource, false);
            this.treeTable.setParent(resource, folder);
        }

    }

    protected void addDependenciesToFolder(String folderName, ProjectVersion projectVersion) {
        FolderName folder = null;
        List<ProjectVersionDepends> dependencies = context.getUiCache().findProjectDependencies(projectVersion.getId());
        for (ProjectVersionDepends dependency : dependencies) {
            if (folder == null) {
                folder = addVirtualFolder(folderName, projectVersion);
            }
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(dependency);
            this.treeTable.setItemIcon(dependency, Icons.DEPENDENCY);
            this.treeTable.setParent(dependency, folder);
            this.treeTable.setChildrenAllowed(dependency, false);
        }
    }

    protected void addFlowsToFolder(String folderName, ProjectVersion projectVersion, boolean test) {
        List<FlowName> flows = context.getUiCache().findFlowsInProject(projectVersion.getId());
        FolderName folder = null;
        for (FlowName flow : flows) {
            if (test == flow.isTest()) {
                if (folder == null) {
                    folder = addVirtualFolder(folderName, projectVersion);
                }
                this.treeTable.setChildrenAllowed(folder, true);
                this.treeTable.addItem(flow);
                this.treeTable.setItemIcon(flow, flow.isWebService() ? Icons.WEB : Icons.FLOW);
                this.treeTable.setParent(flow, folder);
                this.treeTable.setChildrenAllowed(flow, false);
            }
        }

    }

    protected void addModelsToFolder(String folderName, ProjectVersion projectVersion) {
        List<ModelName> models = context.getUiCache().findModelsInProject(projectVersion.getId());
        FolderName folder = null;
        for (ModelName model : models) {
            if (folder == null) {
                folder = addVirtualFolder(folderName, projectVersion);
            }
            this.treeTable.setChildrenAllowed(folder, true);
            this.treeTable.addItem(model);
            this.treeTable.setItemIcon(model, Icons.MODEL);
            this.treeTable.setParent(model, folder);
            this.treeTable.setChildrenAllowed(model, false);
        }
    }

    protected void expand(Object value) {
        if (value != null && !treeTable.containsId(value)) {
            String projectVersionId = null;
            if (value instanceof AbstractName) {
                AbstractName named = (AbstractName) value;
                projectVersionId = named.getProjectVersionId();
            } else if (value instanceof ProjectVersion) {
                projectVersionId = ((ProjectVersion) value).getId();
            }

            String projectId = null;
            if (isNotBlank(projectVersionId)) {
                ProjectVersion projectVersion = configurationService.findProjectVersion(projectVersionId);
                projectId = projectVersion.getProjectId();
            } else if (value instanceof Project) {
                projectId = ((Project) value).getId();
            }

            if (isNotBlank(projectId)) {
                Collection<?> items = treeTable.getItemIds();
                for (Object object : items) {
                    if (object instanceof Project && ((Project) object).getId().equals(projectId) && !((Project) object).isDeleted()) {
                        addProjectVersions(((Project) object));
                    }
                }
            }
        }
        if (value != null) {
            treeTable.setCollapsed(value, false);
            Object parent = treeTable.getParent(value);
            while (parent != null) {
                treeTable.setCollapsed(parent, false);
                parent = treeTable.getParent(parent);
            }
        }
        treeTable.focus();
    }

    public void unselectAll() {
        treeTable.setValue(null);
    }

    public void doOpen() {
        open(treeTable.getValue());
    }

    protected void open(Object item) {
        if (item instanceof FlowName) {
            FlowName flow = (FlowName) item;
            long ts = System.currentTimeMillis();
            EditFlowPanel flowLayout = new EditFlowPanel(context, flow.getId(), this, tabs);
            log.debug("It took {}ms to create the edit flow panel", (System.currentTimeMillis() - ts));
            tabs.addCloseableTab(flow.getId(), flow.getName(), Icons.FLOW, flowLayout);
        } else if (item instanceof ModelName) {
            ModelName modelName = (ModelName) item;
            ProjectVersion projectVersion = findProjectVersion(modelName);
            Model model = configurationService.findModel(modelName.getId());
            if (model.getType().equalsIgnoreCase(Model.TYPE_RELATIONAL)) {
	            EditRelationalModelPanel editModel = new EditRelationalModelPanel(context, modelName.getId(), context.isReadOnly(projectVersion, Privilege.DESIGN));
	            tabs.addCloseableTab(modelName.getId(), modelName.getName(), Icons.MODEL, editModel);
            } else {
            		EditHierarchicalModelPanel editModel = new EditHierarchicalModelPanel(context, modelName.getId(), context.isReadOnly(projectVersion, Privilege.DESIGN));
	            tabs.addCloseableTab(modelName.getId(), modelName.getName(), Icons.MODEL, editModel);
            }
        } else if (item instanceof ResourceName) {
            ResourceName resource = (ResourceName) item;
            ProjectVersion projectVersion = findProjectVersion(resource);
            PropertySheet sheet = new PropertySheet(context, tabs, context.isReadOnly(projectVersion, Privilege.DESIGN));
            sheet.setSource(configurationService.findResource(resource.getId()));
            tabs.addCloseableTab(resource.getId(), resource.getName(), treeTable.getItemIcon(item), sheet);
        } else if (item instanceof ProjectVersion) {
            ProjectVersion projectVersion = (ProjectVersion) item;
            ProjectVersionSettingsPanel panel = new ProjectVersionSettingsPanel(projectVersion, context, this);
            tabs.addCloseableTab(projectVersion.getId(),
                    String.format("%s (%s)", projectVersion.getProject().getName(), projectVersion.getName()), Icons.PROJECT_VERSION, panel);
        }
    }

    public void doExport() {
        ExportDialog.show(context, treeTable.getValue());
    }
    
    public void doTag() {
        TagDialog.show(context, treeTable.getValue());
    }

    public void doChangeDependencyVersion() {
        ChangeDependencyVersionDialog.show(this, context, treeTable.getValue());
        refresh();
    }

    public void doImport() {
        ImportDialog.show("Import Config", "Click the upload button to import your config", new ImportConfigurationListener());
    }

    public void doNewProjectBranch() {
        Object object = treeTable.getValue();
        if (object instanceof ProjectVersion) {
            ProjectVersion original = (ProjectVersion) object;
            configurationService.refresh(original.getProject());
            List<ProjectVersion> versions = original.getProject().getProjectVersions();
            for (ProjectVersion version : versions) {
                if (version.getVersionType().equalsIgnoreCase(ProjectVersion.VersionType.BRANCH.toString())) {
                    CommonUiUtils.notify("Existing branch already exists for this project.  Cannot create a new one.", Type.WARNING_MESSAGE);
                    return;
                }
            }
            ProjectVersion newVersion = configurationService.saveNewVersion("branch", original, "branch");
            newVersion = configurationService.findProjectVersion(newVersion.getId());
            newVersion.setReleaseDate(null);
            configurationService.save(newVersion);
            context.getDefinitionFactory().refresh(newVersion.getId());
            treeTable.addItem(newVersion);
            treeTable.setItemIcon(newVersion, Icons.PROJECT_VERSION);
            treeTable.setParent(newVersion, treeTable.getParent(object));
            treeTable.setChildrenAllowed(newVersion, false);
            treeTable.setValue(newVersion);
            refreshProjects();
        }
    }

    public void doCut() {
        Object object = treeTable.getValue();
        cutCopyPasteManager.cut(object);
    }

    public void doCopy() {
        Object object = treeTable.getValue();
        cutCopyPasteManager.copy(object);
    }

    public void doPaste() {
        Object object = treeTable.getValue();

        String newProjectVersionId = null;
        if (object instanceof FolderName) {
            FolderName folderName = (FolderName) object;
            newProjectVersionId = folderName.getProjectVersionId();
        } else if (object instanceof ResourceName) {
            ResourceName resourceName = (ResourceName) object;
            newProjectVersionId = resourceName.getProjectVersionId();
        } else if (object instanceof ModelName) {
            ModelName modelName = (ModelName) object;
            newProjectVersionId = modelName.getProjectVersionId();
        } else if (object instanceof FlowName) {
            FlowName flowName = (FlowName) object;
            newProjectVersionId = flowName.getProjectVersionId();
        } else if (object instanceof ProjectVersion) {
            ProjectVersion projectVersion = (ProjectVersion) object;
            newProjectVersionId = projectVersion.getId();
        }

        if (newProjectVersionId != null) {
            if (context.getClipboard().containsKey(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE)) {
                if (context.getClipboard().get(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE).equals(Model.class)) {
                    cutCopyPasteManager.pasteModels(newProjectVersionId);
                } else if (context.getClipboard().get(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE).equals(Resource.class)) {
                    cutCopyPasteManager.pasteResources(newProjectVersionId);
                } else if (context.getClipboard().get(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE).equals(Flow.class)) {
                    cutCopyPasteManager.pasteFlow(newProjectVersionId);
                }
                refresh();
            }
        }
    }

    public void doRemove() {
        Object object = treeTable.getValue();
        if (object instanceof FlowName) {
            FlowName flow = (FlowName) object;
            ConfirmDialog.show("Delete Flow?", "Are you sure you want to delete the '" + flow.getName() + "' flow?",
                    new DeleteFlowConfirmationListener(flow));
        } else if (object instanceof ResourceName) {
            ResourceName resource = (ResourceName) object;
            ConfirmDialog.show("Delete Resource?", "Are you sure you want to delete the '" + resource.getName() + "' resource?",
                    new DeleteResourceConfirmationListener(resource));

        } else if (object instanceof ModelName) {
            ModelName model = (ModelName) object;
            if (!configurationService.isModelUsed(model.getId())) {
                ConfirmDialog.show("Delete Model?", "Are you sure you want to delete the '" + model.getName() + "' model?",
                        new DeleteModelConfirmationListener(model));
            } else {
                CommonUiUtils.notify("The model is currently in use.  It cannot be deleted.", Type.WARNING_MESSAGE);
            }
        } else if (object instanceof Project) {
            Project namedObject = (Project) object;
            ConfirmDialog.show("Delete Project?", "Are you sure you want to delete the '" + namedObject.getName() + "' project?",
                    new DeleteProjectConfirmationListener(namedObject));

        } else if (object instanceof ProjectVersion) {
            ProjectVersion namedObject = (ProjectVersion) object;
            ConfirmDialog.show("Delete Project Version?", "Are you sure you want to delete the '" + namedObject.getName() + "' version?",
                    new DeleteProjectVersionConfirmationListener(namedObject));
        } else if (object instanceof ProjectVersionDepends) {
            configurationService.delete((ProjectVersionDepends) object);
            treeTable.removeItem(object);
        }
    }

    protected FolderName findFolderWithName(String name) {
        Object value = treeTable.getValue();
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeTable.getParent(value);
        }

        if (value instanceof ProjectVersion) {
            Collection<?> children = treeTable.getChildren(value);
            if (children != null) {
                for (Object object : children) {
                    if (object instanceof FolderName) {
                        FolderName folder = (FolderName) object;
                        if (folder.getName().equals(name)) {
                            return folder;
                        }
                    }
                }
            }
        }
        return null;
    }

    public ProjectVersion findProjectVersion() {
        Object value = treeTable.getValue();
        return findProjectVersion(value);
    }

    public ProjectVersion findProjectVersion(Object value) {
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeTable.getParent(value);
        }

        if (value instanceof ProjectVersion) {
            return ((ProjectVersion) value);
        } else {
            return null;
        }
    }

    public void promptForNewDependency() {
        SelectProjectVersionDialog.show(context, findProjectVersion().getProject(), v -> addNewDependency(v),
                "Please select a project version that this project depends upon.");
    }

    public void addNewDependency(ProjectVersion targetVersion) {
        ProjectVersion projectVersion = findProjectVersion();
        List<ProjectVersionDepends> dependencies = configurationService.findProjectDependencies(projectVersion.getId());
        boolean add = true;
        for (ProjectVersionDepends projectVersionDependency : dependencies) {
            if (projectVersionDependency.getTargetProjectVersionId().equals(targetVersion.getId())) {
                add = false;
            }
        }

        if (add) {
            ProjectVersionDepends dependency = new ProjectVersionDepends();
            dependency.setProjectVersionId(projectVersion.getId());
            dependency.setTargetProjectVersion(targetVersion);
            configurationService.save(dependency);

            FolderName folder = findFolderWithName(LABEL_DEPENDENCIES);
            if (folder == null) {
                folder = addVirtualFolder(LABEL_DEPENDENCIES, projectVersion);
            }
            treeTable.setChildrenAllowed(folder, true);

            treeTable.addItem(dependency);
            treeTable.setItemIcon(dependency, Icons.DEPENDENCY);
            treeTable.setParent(dependency, folder);
            treeTable.setChildrenAllowed(dependency, false);

            treeTable.setCollapsed(folder, false);
            treeTable.setCollapsed(projectVersion, false);
            treeTable.setValue(dependency);
        }
    }

    public void addNewFlow(boolean testFlow) {
        ProjectVersion projectVersion = findProjectVersion();
        String name = testFlow ? "Tests" : "Flows";
        FolderName folder = findFolderWithName(name);
        if (folder == null) {
            folder = addVirtualFolder(name, projectVersion);
        }
        treeTable.setChildrenAllowed(folder, true);

        FlowName flow = new FlowName();
        flow.setProjectVersionId(projectVersion.getId());
        flow.setName("New Flow");
        flow.setTest(testFlow);
        configurationService.save(flow);

        treeTable.addItem(flow);
        treeTable.setItemIcon(flow, Icons.FLOW);
        treeTable.setParent(flow, folder);
        treeTable.setChildrenAllowed(flow, false);

        treeTable.setCollapsed(folder, false);
        treeTable.setCollapsed(projectVersion, false);
        treeTable.setValue(flow);

        startEditingItem(flow);
    }

    public void addNewDatabase() {
        addNewResource("Database", "Database", Icons.DATABASE);
    }

    public void addNewFtpFileSystem() {
        addNewResource("Ftp", "FTP Directory", Icons.FILE_SYSTEM);
    }
    
    public void addNewJmsSubscribe() {
        addNewResource("JMS Subscribe", "JMS Subscribe", Icons.QUEUE);
    }    

    public void addNewLocalFileSystem() {
        addNewResource("Local File System", "Directory", Icons.FILE_SYSTEM);
    }

    public void addNewSftpFileSystem() {
        addNewResource("Sftp", "SFTP Directory", Icons.FILE_SYSTEM);
    }

    public void addNewJMSFileSystem() {
        addNewResource("JMS", "JMS Directory", Icons.QUEUE);
    }

    public void addNewSMBFileSystem() {
        addNewResource("SMB", "SMB Directory", Icons.FILE_SYSTEM);
    }

    public void addNewHttpResource() {
        addNewResource("Http", "Http", Icons.WEB);
    }

    public void addNewMailSession() {
        addNewResource("MailSession", "Mail Session", Icons.EMAIL);
    }

    protected void addNewResource(String type, String defaultName, FontAwesome icon) {
        ProjectVersion projectVersion = findProjectVersion();
        FolderName folder = findFolderWithName(LABEL_RESOURCES);
        if (folder == null) {
            folder = addVirtualFolder(LABEL_RESOURCES, projectVersion);
        }
        treeTable.setChildrenAllowed(folder, true);

        ResourceName resource = new ResourceName();
        resource.setName(defaultName);
        resource.setProjectVersionId(projectVersion.getId());
        resource.setType(type);
        configurationService.save(resource);

        treeTable.addItem(resource);
        treeTable.setItemIcon(resource, icon);
        treeTable.setParent(resource, folder);
        treeTable.setChildrenAllowed(resource, false);

        treeTable.setCollapsed(folder, false);

        startEditingItem(resource);
    }

    public void addNewModel(String type) {
        ProjectVersion projectVersion = findProjectVersion();
        FolderName folder = findFolderWithName(LABEL_MODELS);
        if (folder == null) {
            folder = addVirtualFolder(LABEL_MODELS, projectVersion);
        }
        treeTable.setChildrenAllowed(folder, true);

        ModelName model = new ModelName();
        model.setName("New Model");
        model.setProjectVersionId(projectVersion.getId());
        model.setType(type);
        
        configurationService.save(model);

        treeTable.addItem(model);
        treeTable.setItemIcon(model, Icons.MODEL);
        treeTable.setParent(model, folder);
        treeTable.setChildrenAllowed(model, false);

        treeTable.setCollapsed(folder, false);

        startEditingItem(model);
    }

    public ApplicationContext getContext() {
        return this.context;
    }

    class ImportConfigurationListener implements IImportListener {

        @Override
        public void onFinished(String dataToImport) {
            context.getImportExportService().importConfiguration(dataToImport, context.getUser().getLoginId());
            context.getDefinitionFactory().refresh();
            refresh();
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
            configurationService.deleteFlow(configurationService.findFlow(toDelete.getId()));
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
            configurationService.delete(configurationService.findResource(toDelete.getId()));
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
            configurationService.delete(configurationService.findModel(toDelete.getId()));
            tabs.closeTab(toDelete.getId());
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);

            return true;
        }
    }

    class DeleteProjectConfirmationListener implements IConfirmListener {

        Project toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteProjectConfirmationListener(Project toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            toDelete.setDeleted(true);
            configurationService.save(toDelete);
            tabs.closeAll();
            treeTable.setValue(null);
            refresh();
            return true;
        }
    }

    class DeleteProjectVersionConfirmationListener implements IConfirmListener {

        ProjectVersion toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteProjectVersionConfirmationListener(ProjectVersion toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
            toDelete.setDeleted(true);
            configurationService.save(toDelete);
            tabs.closeAll();
            Object parent = treeTable.getParent(toDelete);
            refresh();
            treeTable.setValue(parent);
            treeTable.setCollapsed(parent, false);
            return true;
        }
    }
}
