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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.views.DesignNavigator;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ManageProjectsPanel extends VerticalLayout implements IUiPanel {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    DesignNavigator projectNavigator;

    TreeTable treeTable;

    Button openProjectButton;

    Button newProjectButton;

    Button newVersionButton;

    Button editButton;

    Button removeButton;

    AbstractObject currentlyEditing;

    ShortcutListener enterKeyListener;

    public ManageProjectsPanel(ApplicationContext context, DesignNavigator projectNavigator) {
        this.setSizeFull();
        this.context = context;
        this.projectNavigator = projectNavigator;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        openProjectButton = buttonBar.addButton("Open Project", Icons.PROJECT);
        openProjectButton.addClickListener(event -> openProject(treeTable.getValue()));

        newProjectButton = buttonBar.addButton("New Project", Icons.PROJECT);
        newProjectButton.addClickListener(event -> createNewProject());

        newVersionButton = buttonBar.addButton("New Version", Icons.VERSION);
        newVersionButton.addClickListener(event -> createNewVersion());
        newVersionButton.setEnabled(false);
        newVersionButton.setDescription("Not yet supported");

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(event -> edit(treeTable.getValue()));

        removeButton = buttonBar.addButton("Remove", Icons.DELETE);
        removeButton.addClickListener(event -> removeProject());

        enterKeyListener = new ShortcutListener("Enter", KeyCode.ENTER, null) {
            public void handleAction(Object sender, Object target) {
                if (currentlyEditing != null) {
                    save();
                } else {
                    openProject(treeTable.getValue());
                }
            }
        };

        treeTable = new TreeTable();
        treeTable.setSizeFull();
        treeTable.setSortEnabled(true);
        treeTable.setCacheRate(100);
        treeTable.setImmediate(true);
        treeTable.setSelectable(true);
        treeTable.setEditable(true);
        treeTable.addContainerProperty("name", String.class, "", "Name", null, null);
        treeTable.addContainerProperty("description", String.class, "", "Description", null, null);
        treeTable.addContainerProperty("locked", Boolean.class, null, "Locked", null, null);
        treeTable.setColumnWidth("locked", 45);
        treeTable.addContainerProperty("archived", Boolean.class, null, "Archived", null, null);
        treeTable.addContainerProperty("createTime", Date.class, null, "Create Time", null, null);
        treeTable.setColumnCollapsingAllowed(true);
        treeTable.setColumnCollapsed("archived", true);
        treeTable.addItemClickListener(new TreeTableItemClickListener());
        treeTable.addValueChangeListener(event -> save());
        treeTable.setTableFieldFactory(new FieldFactory());
        treeTable.setSortContainerPropertyId("name");
        treeTable.setSortAscending(true);

        addComponent(treeTable);
        setExpandRatio(treeTable, 1);

        refresh();

    }

    protected void setButtonsEnabled() {
        Object obj = treeTable.getValue();
        removeButton.setEnabled(obj != null);
        openProjectButton.setEnabled(obj != null);
        editButton.setEnabled(obj != null);
        newVersionButton.setEnabled(obj instanceof ProjectVersion);

        removeButton.setEnabled(currentlyEditing == null);
        openProjectButton.setEnabled(currentlyEditing == null);
        editButton.setEnabled(currentlyEditing == null);
        newProjectButton.setEnabled(currentlyEditing == null);
        newVersionButton.setEnabled(currentlyEditing == null);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
        treeTable.addShortcutListener(enterKeyListener);
    }

    @Override
    public void deselected() {
        treeTable.removeShortcutListener(enterKeyListener);
    }

    protected void openProject(Object source) {
        Object selected = source != null ? source : treeTable.getValue();
        if (selected instanceof Project) {
            selected = ((Project) selected).getLatestProjectVersion();
        }

        if (selected instanceof ProjectVersion) {
            projectNavigator.addProjectVersion((ProjectVersion) selected);
        }
    }

    protected void refresh() {
        Object selected = treeTable.getValue();

        IConfigurationService configurationService = context.getConfigurationService();
        addAll(configurationService.findProjects());

        setButtonsEnabled();

        if (selected == null && treeTable.size() > 0) {
            selected = treeTable.getItemIds().iterator().next();
        }
        treeTable.setValue(selected);

        Object parent = treeTable.getParent(selected);
        if (parent != null) {
            treeTable.setCollapsed(parent, false);
        }

    }

    protected void addAll(List<Project> projects) {
        treeTable.removeAllItems();
        for (Project project : projects) {
            add(project);
        }

        treeTable.sort();
    }

    protected void add(Project project) {
        treeTable.addItem(new Object[] { project.getName(), project.getDescription(), null, null, project.getCreateTime() }, project);
        treeTable.setItemIcon(project, Icons.PROJECT);
        List<ProjectVersion> versions = project.getProjectVersions();
        for (ProjectVersion projectVersion : versions) {
            addToTable(project, projectVersion);
        }
    }

    protected void addToTable(Project project, ProjectVersion projectVersion) {
        treeTable.setChildrenAllowed(project, true);
        treeTable.addItem(new Object[] { projectVersion.getVersionLabel(), projectVersion.getDescription(), projectVersion.isLocked(),
                projectVersion.isArchived(), project.getCreateTime() }, projectVersion);
        treeTable.setItemIcon(projectVersion, Icons.VERSION);
        treeTable.setParent(projectVersion, project);
        treeTable.setChildrenAllowed(projectVersion, false);
    }

    protected void edit(Object obj) {
        treeTable.setValue(obj);
        if (currentlyEditing == null) {
            currentlyEditing = (AbstractObject) obj;
        }
        setButtonsEnabled();
        treeTable.refreshRowCache();
    }

    protected void selectPrevious() {
        Object previous = null;
        Object selected = treeTable.getValue();
        Collection<?> items = treeTable.getItemIds();
        for (Object object : items) {
            if (object == selected) {
                break;
            }
            previous = object;
        }
        treeTable.setValue(previous);
    }

    protected void createNewVersion() {
        CommonUiUtils.notify("Not implemented.  Coming Soon.", Type.HUMANIZED_MESSAGE);
    }

    protected void save() {
        Item item = treeTable.getItem(currentlyEditing);
        if (item != null) {
            IConfigurationService configurationService = context.getConfigurationService();

            String name = (String) item.getItemProperty("name").getValue();
            String desc = (String) item.getItemProperty("description").getValue();
            if (currentlyEditing instanceof Project) {
                Project project = (Project) currentlyEditing;
                project.setName(name);
                project.setDescription(desc);
                configurationService.save(project);
                projectNavigator.refresh();

            } else if (currentlyEditing instanceof ProjectVersion) {
                ProjectVersion version = (ProjectVersion) currentlyEditing;
                version.setVersionLabel(name);
                version.setDescription(desc);

                Boolean locked = (Boolean) item.getItemProperty("locked").getValue();
                locked = locked == null ? false : locked;
                version.setLocked(locked);

                Boolean archived = (Boolean) item.getItemProperty("archived").getValue();
                archived = archived == null ? false : archived;
                version.setArchived(archived);
                configurationService.save(version);
                projectNavigator.refresh();
            }

            currentlyEditing = null;
            setButtonsEnabled();
            treeTable.setSortContainerPropertyId(treeTable.getSortContainerPropertyId());
            treeTable.refreshRowCache();
            treeTable.focus();
        }
    }
    
    protected void createNewProject() {
        Project project = new Project();
        project.setName("New Project");
        ProjectVersion version = new ProjectVersion();
        version.setVersionLabel("1.0");
        version.setProject(project);
        project.getProjectVersions().add(version);
        IConfigurationService configurationService = context.getConfigurationService();
        configurationService.save(project);
        configurationService.save(version);
        refresh();
        edit(project);
    }
    
    protected void removeProject () {
        ConfirmDialog.show("Delete Project?", "Are you sure you want to delete the selected project?", new IConfirmListener() {
            @Override
            public boolean onOk() {
                Object selected = treeTable.getValue();
                if (selected instanceof ProjectVersion) {
                    ProjectVersion projectVersion = (ProjectVersion) selected;
                    projectVersion.setDeleted(true);
                    context.getConfigurationService().save(projectVersion);

                    if (projectVersion.getProject().getProjectVersions().size() <= 1) {
                        selected = projectVersion.getProject();
                    }
                }

                if (selected instanceof Project) {
                    Project project = (Project) selected;
                    project.setDeleted(true);
                    context.getConfigurationService().save(project);
                }

                selectPrevious();
                refresh();
                projectNavigator.refresh();
                return true;
            }
        });
    }

    class TreeTableItemClickListener implements ItemClickListener {
        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                openProject(event.getItemId());
            }
        }
    }

    class FieldFactory extends DefaultFieldFactory {
        @Override
        public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {
            boolean isVersion = itemId instanceof ProjectVersion;
            if (itemId.equals(currentlyEditing) && !propertyId.equals("createTime")
                    && !(!isVersion && (propertyId.equals("locked") || propertyId.equals("archived")))) {
                Field<?> field = super.createField(container, itemId, propertyId, uiContext);
                if (field instanceof TextField) {
                    final TextField textField = (TextField) field;
                    textField.setNullRepresentation("");
                    textField.setWidth(100, Unit.PERCENTAGE);
                    textField.addFocusListener(new FocusListener() {

                        @Override
                        public void focus(FocusEvent event) {
                            textField.selectAll();
                        }
                    });
                    if ("name".equals(propertyId)) {
                        textField.focus();
                    }

                } else if (field instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) field;
                    checkBox.setCaption(null);
                    checkBox.setWidth(100, Unit.PERCENTAGE);
                }

                return field;
            } else {
                return null;
            }
        }
    }

}
