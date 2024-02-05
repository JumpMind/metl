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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jumpmind.metl.core.model.AbstractName;
import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.HierarchicalModel;
import org.jumpmind.metl.core.model.HierarchicalModelName;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.RelationalModelName;
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.value.ValueChangeMode;

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

    TreeGrid<AbstractNamedObject> treeGrid;

    AbstractNamedObject itemBeingEdited;

    DesignMenuBar menuBar;

    IConfigurationService configurationService;

    CutCopyPasteManager cutCopyPasteManager;

    String tagFilterText = null;

    public DesignNavigator(ApplicationContext context, TabbedPanel tabs) {
        this.context = context;
        this.tabs = tabs;
        this.configurationService = context.getConfigurationService();
        this.cutCopyPasteManager = new CutCopyPasteManager(context);

        setSizeFull();
        buildTreeGrid();
        HorizontalLayout hLayout = new HorizontalLayout();
        menuBar = new DesignMenuBar(this, treeGrid);
        hLayout.add(menuBar, buildFilterField());
        hLayout.setSpacing(true);
        add(hLayout);
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
        filterField.setPrefixComponent(new Icon(Icons.SEARCH));
        filterField.setPlaceholder("Tag Filter");
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(200);
        filterField.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            public void valueChanged(ValueChangeEvent<String> event) {
                if (event.getValue() != null && !event.getValue().isEmpty()) {
                    tagFilterText = event.getValue();
                } else {
                    tagFilterText = null;
                }
                refreshProjects();
            }
        });
        return filterField;
    }

    protected TreeGrid<AbstractNamedObject> buildTreeGrid() {
        treeGrid = new TreeGrid<AbstractNamedObject>();
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        treeGrid.addClassName("noselect");
        treeGrid.setSizeFull();
        treeGrid.setPageSize(100);
        Editor<AbstractNamedObject> editor = treeGrid.getEditor();
        Binder<AbstractNamedObject> binder = new Binder<AbstractNamedObject>();
        editor.setBinder(binder);
        editor.addOpenListener(event -> itemBeingEdited = event.getItem());
        editor.addCancelListener(event -> abortEditingItem());
        editor.addSaveListener(event -> finishEditingItem(event.getItem().getName()));
        EnableFocusTextField editorField = buildEditableNavigatorField();
        binder.forField(editorField).bind(AbstractNamedObject::getName, AbstractNamedObject::setName);
        treeGrid.addComponentColumn(item -> {
            Icon icon = null;
            if (item instanceof FolderName) {
                icon = new Icon(treeGrid.isExpanded(item) ? Icons.FOLDER_OPEN : Icons.FOLDER_CLOSED);
            } else if (item instanceof ProjectVersion) {
                icon = new Icon(((ProjectVersion) item).locked() ? VaadinIcon.LOCK : Icons.PROJECT_VERSION);
            } else if (item instanceof Project) {
                icon = new Icon(Icons.PROJECT);
            } else if (item instanceof ResourceName) {
                icon = getIconForResource((ResourceName) item);
            } else if (item instanceof ProjectVersionDepends) {
                icon = new Icon(Icons.DEPENDENCY);
            } else if (item instanceof FlowName) {
                icon = new Icon(((FlowName) item).isWebService() ? Icons.WEB : Icons.FLOW);
            } else if (item instanceof RelationalModelName || item instanceof HierarchicalModelName) {
                icon = new Icon(Icons.MODEL);
            }
            Span span = new Span(item.getName());
            if (icon != null) {
                return new HorizontalLayout(icon, span);
            }
            return span;
        }).setEditorComponent(editorField).setFlexGrow(1);
        treeGrid.addItemClickListener(event -> {
            if (event.getButton() == 0) {
                if (event.getClickCount() == 2) {
                    abortEditingItem();
                    open(event.getItem());
                    if (treeGrid.getDataProvider().hasChildren(event.getItem())) {
                        AbstractNamedObject item = event.getItem();
                        if (treeGrid.isExpanded(item)) {
                            treeGrid.collapse(item);
                        } else {
                            treeGrid.expand(item);
                        }
                    }
                }
            }
        });
        treeGrid.addExpandListener(e -> {
            // deselect any selected rows when they expand or collapse?
            treeGrid.deselectAll();
            Collection<AbstractNamedObject> items = e.getItems();
            if (!items.isEmpty()) {
                AbstractNamedObject item = items.iterator().next();
                if (item instanceof Project && !treeGrid.getDataProvider().hasChildren(item)) {
                    addProjectVersions((Project) item);
                }
            }

            saveExpandedList();
        });
        treeGrid.addCollapseListener(event -> saveExpandedList());
        treeGrid.setClassNameGenerator(itemId -> {
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
            return null;
        });
        treeGrid.addSelectionListener(e -> selectionChanged());

        return treeGrid;
    }
    
    protected Icon getIconForResource(ResourceName resource) {
        if ("Database".equals(resource.getType())) {
            return new Icon(Icons.DATABASE);
        } else if ("Http".equals(resource.getType())) {
            return new Icon(Icons.WEB);
        } else if ("MailSession".equals(resource.getType())) {
            return new Icon(Icons.EMAIL);
        } else if (resource.getType().contains("JMS") || resource.getType().contains("SQS")) {
            return new Icon(Icons.QUEUE);
        } else if (resource.getType().contains("S3")) {
            return new Icon(Icons.CLOUD);
        } else {
            return new Icon(Icons.FILE_SYSTEM);
        }
    }

    protected void saveExpandedList() {
        if (!rebuilding) {
            StringBuilder ids = new StringBuilder();
            List<AbstractNamedObject> itemIds = getAllItems();
            for (AbstractNamedObject itemId : itemIds) {
                if (treeGrid.isExpanded(itemId)) {
                    if (itemId instanceof FolderName) {
                        List<AbstractNamedObject> children = treeGrid.getTreeData().getChildren(itemId);
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
    
    protected List<AbstractNamedObject> getAllItems() {
        List<AbstractNamedObject> itemList = new ArrayList<AbstractNamedObject>();
        addItemsRecursively(null, itemList);
        return itemList;
    }
    
    protected void addItemsRecursively(AbstractNamedObject item, List<AbstractNamedObject> list) {
        if (item != null) {
            list.add(item);
        }
        for (AbstractNamedObject child : treeGrid.getTreeData().getChildren(item)) {
            addItemsRecursively(child, list);
        }
    }

    protected void selectionChanged() {
        AbstractObject object = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
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

    protected EnableFocusTextField buildEditableNavigatorField() {
        final EnableFocusTextField field = new EnableFocusTextField();
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        field.setWidth("95%");
        field.addFocusListener(e -> {
            field.setFocusAllowed(false);
            field.getElement().executeJs("this.inputElement.select()");
            field.setFocusAllowed(true);
        });
        field.focus();
        return field;
    }

    public boolean startEditingItem(AbstractNamedObject obj) {
        if (obj.isSettingNameAllowed()) {
            itemBeingEdited = obj;
            treeGrid.getDataProvider().refreshAll();
            treeGrid.deselectAll();
            treeGrid.getEditor().editItem(obj);
            return true;
        } else {
            return false;
        }
    }

    protected void finishEditingItem(String value) {
        if (itemBeingEdited != null && isNotBlank(value)) {
            itemBeingEdited.setName(value);
            AbstractNamedObject selected = itemBeingEdited;
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
            treeGrid.select(selected);
            refreshProjects();
        }
    }

    protected void abortEditingItem() {
        if (itemBeingEdited != null) {
            itemBeingEdited = null;
            refresh();
            treeGrid.focus();
        }
    }

    public void refresh() {
        refreshProjects();

        menuBar.refresh();

        boolean add = true;
        Iterator<Component> i = getChildren().iterator();
        while (i.hasNext()) {
            if (i.next().equals(treeGrid)) {
                add = false;
                break;
            }
        }

        if (add) {
            add(treeGrid);
            expand(treeGrid);
        }
        treeGrid.getDataProvider().refreshAll();
    }

    protected void addProjectVersions(Project project) {
        List<ProjectVersion> versions = project.getProjectVersions();
        for (ProjectVersion projectVersion : versions) {
            treeGrid.getTreeData().addItem(project, projectVersion);

            addFlowsToFolder(LABEL_FLOWS, projectVersion, false);
            addFlowsToFolder(LABEL_TESTS, projectVersion, true);
            addModelsToFolder(LABEL_MODELS, projectVersion);
            addResourcesToFolder(LABEL_RESOURCES, projectVersion);
            addDependenciesToFolder(LABEL_DEPENDENCIES, projectVersion);
        }
    }

    protected void collapseAll(AbstractNamedObject itemId) {
        treeGrid.collapseRecursively(Stream.of(itemId), 1);
    }

    protected void refreshProjects() {
        rebuilding = true;
        try {
            long ts = System.currentTimeMillis();
            AbstractNamedObject selected = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
            List<Project> projects;
            if (StringUtils.isBlank(tagFilterText)) {
                projects = configurationService.findProjects();
            } else {
                projects = configurationService.findProjectsWithTagLike(tagFilterText);
            }
            List<AbstractNamedObject> itemIds = getAllItems();
            for (AbstractNamedObject itemId : itemIds) {
                collapseAll(itemId);
            }
            treeGrid.getTreeData().clear();
            for (Project project : projects) {
                treeGrid.getTreeData().addItem(null, project);
            }

            if (selected == null) {
                String selectedId = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_SELECTION_ID).getValue();
                if (isNotBlank(selectedId)) {
                    String projectId = context.getUser().findSetting(UserSetting.SETTING_DESIGN_NAVIGATOR_SELECTED_PROJECT_ID).getValue();
                    if (isNotBlank(projectId)) {
                        for (AbstractNamedObject object : itemIds) {
                            if (object instanceof Project && ((Project) object).getId().equals(projectId)
                                    && !((Project) object).isDeleted() && projects.contains(object)) {
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
                    AbstractNamedObject object = null;
                    if (type.equals(FlowName.class.getSimpleName())) {
                        object = new FlowName();
                        object.setId(id);
                    } else if (type.equals(RelationalModelName.class.getSimpleName())) {
                        object = new RelationalModelName();
                        object.setId(id);
                    } else if (type.equals(HierarchicalModelName.class.getSimpleName())) {
                        object = new HierarchicalModelName();
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
                treeGrid.select(selected);
            }
            treeGrid.getDataProvider().refreshAll();
            log.debug("It took {}ms to refresh projects in the design view", (System.currentTimeMillis() - ts));
        } finally {
            rebuilding = false;
        }
    }

    protected AbstractNamedObject findChild(String id, AbstractNamedObject parent) {
        List<AbstractNamedObject> items = treeGrid.getTreeData().getChildren(parent);
        if (items != null) {
            for (AbstractNamedObject object : items) {
                if (id.equals(object.getId())) {
                    return object;
                } else {
                    AbstractNamedObject obj = findChild(id, object);
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

        treeGrid.getTreeData().addItem(projectVersion, folder);
        return folder;
    }

    protected void addResourcesToFolder(String folderName, ProjectVersion projectVersion) {
        FolderName folder = null;
        List<ResourceName> resources = context.getUiCache().findResourcesInProject(projectVersion.getId());
        for (ResourceName resource : resources) {
            if (folder == null) {
                folder = addVirtualFolder(folderName, projectVersion);
            }
            this.treeGrid.getTreeData().addItem(folder, resource);
        }

    }

    protected void addDependenciesToFolder(String folderName, ProjectVersion projectVersion) {
        FolderName folder = null;
        List<ProjectVersionDepends> dependencies = context.getUiCache().findProjectDependencies(projectVersion.getId());
        for (ProjectVersionDepends dependency : dependencies) {
            if (folder == null) {
                folder = addVirtualFolder(folderName, projectVersion);
            }
            this.treeGrid.getTreeData().addItem(folder, dependency);
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
                this.treeGrid.getTreeData().addItem(folder, flow);
            }
        }

    }

    protected void addModelsToFolder(String folderName, ProjectVersion projectVersion) {
        List<RelationalModelName> relationalModels = context.getUiCache().findRelationalModelsInProject(projectVersion.getId());
        FolderName folder = null;
        for (RelationalModelName relationalModel : relationalModels) {
            if (folder == null) {
                folder = addVirtualFolder(folderName, projectVersion);
            }
            this.treeGrid.getTreeData().addItem(folder, relationalModel);
        }
        
        List<HierarchicalModelName> hierarchicalModels = context.getUiCache().findHierarchicalModelsInProject(projectVersion.getId());
        for (HierarchicalModelName hierarchicalModel : hierarchicalModels) {
            if (folder == null) {
                folder = addVirtualFolder(folderName, projectVersion);
            }
            this.treeGrid.getTreeData().addItem(folder, hierarchicalModel);
        }        
    }

    protected void expand(AbstractNamedObject value) {
        List<AbstractNamedObject> items = getAllItems();
        if (value != null && !items.contains(value)) {
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
                for (Object object : items) {
                    if (object instanceof Project && ((Project) object).getId().equals(projectId) && !((Project) object).isDeleted()) {
                        addProjectVersions(((Project) object));
                    }
                }
            }
        }
        if (value != null && treeGrid.getTreeData().contains(value)) {
            treeGrid.expand(value);
            AbstractNamedObject parent = treeGrid.getTreeData().getParent(value);
            while (parent != null) {
                treeGrid.expand(parent);
                parent = treeGrid.getTreeData().getParent(parent);
            }
        }
        treeGrid.focus();
    }

    public void unselectAll() {
        treeGrid.deselectAll();
    }

    public void doOpen() {
        open(treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null));
    }

    protected void open(Object item) {
        if (item instanceof FlowName) {
            FlowName flow = (FlowName) item;
            long ts = System.currentTimeMillis();
            EditFlowPanel flowLayout = new EditFlowPanel(context, flow.getId(), this, tabs);
            log.debug("It took {}ms to create the edit flow panel", (System.currentTimeMillis() - ts));
            tabs.addCloseableTab(flow.getId(), flow.getName(), new Icon(Icons.FLOW), flowLayout);
        } else if (item instanceof RelationalModelName) {
            RelationalModelName modelName = (RelationalModelName) item;
            ProjectVersion projectVersion = findProjectVersion(modelName);
            EditRelationalModelPanel editModel = new EditRelationalModelPanel(context, modelName.getId(),
                    context.isReadOnly(projectVersion, Privilege.DESIGN));
            tabs.addCloseableTab(modelName.getId(), modelName.getName(), new Icon(Icons.MODEL), editModel);        
        } else if (item instanceof HierarchicalModelName){
            HierarchicalModelName modelName = (HierarchicalModelName) item;
            ProjectVersion projectVersion = findProjectVersion(modelName);
            EditHierarchicalModelPanel editModel = new EditHierarchicalModelPanel(context, modelName.getId(),
                    context.isReadOnly(projectVersion, Privilege.DESIGN));
            tabs.addCloseableTab(modelName.getId(), modelName.getName(), new Icon(Icons.MODEL), editModel);
        } else if (item instanceof ResourceName) {
            ResourceName resource = (ResourceName) item;
            ProjectVersion projectVersion = findProjectVersion(resource);
            PropertySheet sheet = new PropertySheet(context, tabs, context.isReadOnly(projectVersion, Privilege.DESIGN));
            sheet.setSource(configurationService.findResource(resource.getId()));
            tabs.addCloseableTab(resource.getId(), resource.getName(), getIconForResource((ResourceName) item), sheet);
        } else if (item instanceof ProjectVersion) {
            ProjectVersion projectVersion = (ProjectVersion) item;
            ProjectVersionSettingsPanel panel = new ProjectVersionSettingsPanel(projectVersion, context, this);
            tabs.addCloseableTab(projectVersion.getId(),
                    String.format("%s (%s)", projectVersion.getProject().getName(), projectVersion.getName()),
                    new Icon(Icons.PROJECT_VERSION), panel);
        }
    }

    public void doWhereUsed() {
        whereUsed(treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null));
    }
    
    protected void whereUsed(Object item) {
    	if (item instanceof RelationalModelName) {
            RelationalModelName modelName = (RelationalModelName) item;
            WhereUsedPanel panel = new WhereUsedPanel("Model", modelName.getId(), modelName.getName(), context, this);
            tabs.addCloseableTab(modelName.getId()+"WU",
                    String.format("%s (Where Used)", modelName.getName()), new Icon(Icons.MODEL), panel);
    	} else if (item instanceof HierarchicalModelName) {
            HierarchicalModelName modelName = (HierarchicalModelName) item;
            WhereUsedPanel panel = new WhereUsedPanel("Model", modelName.getId(), modelName.getName(), context, this);
            tabs.addCloseableTab(modelName.getId()+"WU",
                    String.format("%s (Where Used)", modelName.getName()), new Icon(Icons.MODEL), panel);
    	} else if (item instanceof ResourceName) {
    		ResourceName resource = (ResourceName) item;
            WhereUsedPanel panel = new WhereUsedPanel("Resource", resource.getId(), resource.getName(), context, this);
            tabs.addCloseableTab(resource.getId()+"WU",
                    String.format("%s (Where Used)", resource.getName()), getIconForResource((ResourceName) item), panel);
    	} else if (item instanceof FlowName) {
    		FlowName flow = (FlowName) item;
            WhereUsedPanel panel = new WhereUsedPanel("Flow", flow.getId(), flow.getName(), context, this);
            tabs.addCloseableTab(flow.getId()+"WU",
                    String.format("%s (Where Used)", flow.getName()), new Icon(Icons.FLOW), panel);
        } else if (item instanceof ProjectVersion) {
            ProjectVersion projectVersion = (ProjectVersion) item;
            WhereUsedPanel panel = new WhereUsedPanel("ProjectVersion", projectVersion.getId(),
                    String.format("%s-%s", projectVersion.getProject().getName(), projectVersion.getName()), context, this);
            tabs.addCloseableTab(
                    projectVersion.getId() + "WU", String.format("%s (%s - Where Used)",
                            projectVersion.getProject().getName(), projectVersion.getName()),
                    new Icon(Icons.PROJECT_VERSION), panel);
    	}
    }

    public void doExport() {
        ExportDialog.show(context, treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null));
    }

    public void doTag() {
        TagDialog.show(context, treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null));
    }

    public void doChangeDependencyVersion() {
        ChangeDependencyVersionDialog.show(this, context, treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null));
        refresh();
    }

    public void doImport() {
        ImportDialog.show("Import Config", "Click the upload button to import your config", new ImportConfigurationListener());
    }

    public void doNewProjectBranch() {
        AbstractNamedObject object = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
        if (object instanceof ProjectVersion) {
            ProjectVersion original = (ProjectVersion) object;
            configurationService.refresh(original.getProject());
            List<ProjectVersion> versions = original.getProject().getProjectVersions();
            for (ProjectVersion version : versions) {
                if (version.getVersionType().equalsIgnoreCase(ProjectVersion.VersionType.BRANCH.toString())) {
                    CommonUiUtils.notify("Existing branch already exists for this project.  Cannot create a new one.");
                    return;
                }
            }
            ProjectVersion newVersion = configurationService.saveNewVersion("branch", original, "branch");
            newVersion = configurationService.findProjectVersion(newVersion.getId());
            newVersion.setReleaseDate(null);
            configurationService.save(newVersion);
            context.getDefinitionFactory().refresh(newVersion.getId());
            TreeData<AbstractNamedObject> treeData = treeGrid.getTreeData();
            treeData.addItem(treeData.getParent(object), newVersion);
            treeGrid.select(newVersion);
            refreshProjects();
        }
    }

    public void doCut() {
        Object object = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
        cutCopyPasteManager.cut(object);
    }

    public void doCopy() {
        Object object = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
        cutCopyPasteManager.copy(object);
    }

    public void doPaste() {
        Object object = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);

        String newProjectVersionId = null;
        if (object instanceof FolderName) {
            FolderName folderName = (FolderName) object;
            newProjectVersionId = folderName.getProjectVersionId();
        } else if (object instanceof ResourceName) {
            ResourceName resourceName = (ResourceName) object;
            newProjectVersionId = resourceName.getProjectVersionId();
        } else if (object instanceof RelationalModelName) {
            RelationalModelName modelName = (RelationalModelName) object;
            newProjectVersionId = modelName.getProjectVersionId();
        } else if (object instanceof HierarchicalModelName) {
        	HierarchicalModelName modelName = (HierarchicalModelName) object;
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
                if (context.getClipboard().get(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE).equals(RelationalModel.class) ||
                        context.getClipboard().get(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE).equals(HierarchicalModel.class)) {
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
        AbstractNamedObject object = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
        if (object instanceof FlowName) {
            FlowName flow = (FlowName) object;
            ConfirmDialog.show("Delete Flow?", "Are you sure you want to delete the '" + flow.getName() + "' flow?",
                    new DeleteFlowConfirmationListener(flow));
        } else if (object instanceof ResourceName) {
            ResourceName resource = (ResourceName) object;
            ConfirmDialog.show("Delete Resource?", "Are you sure you want to delete the '" + resource.getName() + "' resource?",
                    new DeleteResourceConfirmationListener(resource));

        } else if (object instanceof RelationalModelName
        		|| object instanceof HierarchicalModelName) {
        	AbstractName model = (AbstractName) object;
            if (!configurationService.isModelUsed(model.getId())) {
                ConfirmDialog.show("Delete Model?", "Are you sure you want to delete the '" + model.getName() + "' model?",
                        new DeleteModelConfirmationListener(model));
            } else {
                CommonUiUtils.notify("The model is currently in use.  It cannot be deleted.");
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
            treeGrid.getTreeData().removeItem(object);
        }
    }

    protected FolderName findFolderWithName(String name) {
        AbstractNamedObject value = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeGrid.getTreeData().getParent(value);
        }

        if (value instanceof ProjectVersion) {
            List<AbstractNamedObject> children = treeGrid.getTreeData().getChildren(value);
            if (children != null) {
                for (AbstractNamedObject object : children) {
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
        AbstractNamedObject value = treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
        return findProjectVersion(value);
    }

    public ProjectVersion findProjectVersion(AbstractNamedObject value) {
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeGrid.getTreeData().getParent(value);
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

            treeGrid.getTreeData().addItem(folder, dependency);

            treeGrid.expand(folder, projectVersion);
            treeGrid.select(dependency);
        }
    }

    public void addNewFlow(boolean testFlow) {
        ProjectVersion projectVersion = findProjectVersion();
        String name = testFlow ? "Tests" : "Flows";
        FolderName folder = findFolderWithName(name);
        if (folder == null) {
            folder = addVirtualFolder(name, projectVersion);
        }

        FlowName flow = new FlowName();
        flow.setProjectVersionId(projectVersion.getId());
        flow.setName("New Flow");
        flow.setTest(testFlow);
        configurationService.save(flow);

        treeGrid.getTreeData().addItem(folder, flow);

        treeGrid.expand(folder, projectVersion);
        treeGrid.select(flow);

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

    public void addNewKafkaProducer() {
    	addNewResource("KafkaProducer", "Kafka Publisher", Icons.QUEUE);
    }
    
    public void addNewSqsQueue() {
    	addNewResource("SQSQueue", "SQS Queue", Icons.QUEUE);
    }
    
    public void addNewMailSession() {
        addNewResource("MailSession", "Mail Session", Icons.EMAIL);
    }
    
    public void addNewAWSS3() {
    	addNewResource("AWS S3", "AWS S3", Icons.CLOUD);
    }

    protected void addNewResource(String type, String defaultName, VaadinIcon icon) {
        ProjectVersion projectVersion = findProjectVersion();
        FolderName folder = findFolderWithName(LABEL_RESOURCES);
        if (folder == null) {
            folder = addVirtualFolder(LABEL_RESOURCES, projectVersion);
        }

        ResourceName resource = new ResourceName();
        resource.setName(defaultName);
        resource.setProjectVersionId(projectVersion.getId());
        resource.setType(type);
        configurationService.save(resource);

        treeGrid.getTreeData().addItem(folder, resource);

        treeGrid.expand(folder);

        startEditingItem(resource);
    }

    public void addNewHierarhicalModel() {
        ProjectVersion projectVersion = findProjectVersion();
        FolderName folder = findFolderWithName(LABEL_MODELS);
        if (folder == null) {
            folder = addVirtualFolder(LABEL_MODELS, projectVersion);
        }

        HierarchicalModelName model = new HierarchicalModelName();
        model.setName("New Model");
        model.setProjectVersionId(projectVersion.getId());

        configurationService.save(model);

        treeGrid.getTreeData().addItem(folder, model);

        treeGrid.expand(folder);

        startEditingItem(model);
    }

    public void addNewRelationalModel() {
        ProjectVersion projectVersion = findProjectVersion();
        FolderName folder = findFolderWithName(LABEL_MODELS);
        if (folder == null) {
            folder = addVirtualFolder(LABEL_MODELS, projectVersion);
        }

        RelationalModelName model = new RelationalModelName();
        model.setName("New Model");
        model.setProjectVersionId(projectVersion.getId());

        configurationService.save(model);

        treeGrid.getTreeData().addItem(folder, model);

        treeGrid.expand(folder);

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
            AbstractNamedObject parent = treeGrid.getTreeData().getParent(toDelete);
            refresh();
            treeGrid.select(parent);
            treeGrid.expand(parent);

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
            AbstractNamedObject parent = treeGrid.getTreeData().getParent(toDelete);
            refresh();
            treeGrid.select(parent);
            treeGrid.expand(parent);
            return true;
        }

    }

    class DeleteModelConfirmationListener implements IConfirmListener {

    	AbstractName toDelete;

        private static final long serialVersionUID = 1L;

        public DeleteModelConfirmationListener(AbstractName toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public boolean onOk() {
        	if (toDelete instanceof HierarchicalModelName) {
        		HierarchicalModel hierModel = configurationService.findHierarchicalModel(toDelete.getId());
            	configurationService.delete(hierModel);
        	} else if (toDelete instanceof RelationalModelName) {
        		RelationalModel relModel = configurationService.findRelationalModel(toDelete.getId());
            	configurationService.delete(relModel);
        	} else {
        		throw new RuntimeException("Request to delete an unknown model type could not be handled.");
        	}
            tabs.closeTab(toDelete.getId());
            AbstractNamedObject parent = treeGrid.getTreeData().getParent(toDelete);
            refresh();
            treeGrid.select(parent);
            treeGrid.expand(parent);

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
            treeGrid.deselectAll();
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
            AbstractNamedObject parent = treeGrid.getTreeData().getParent(toDelete);
            refresh();
            treeGrid.select(parent);
            treeGrid.expand(parent);
            return true;
        }
    }
}
