package org.jumpmind.symmetric.is.ui.views.design;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Project;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.views.ProjectNavigator;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ViewProjectsPanel extends VerticalLayout implements IUiPanel {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    ProjectNavigator projectNavigator;

    TreeTable treeTable;

    Set<Object> lastEditItemIds = Collections.emptySet();

    Button saveButton;

    Button cancelButton;

    Button openProjectButton;

    Button newProjectButton;

    Button newVersionButton;

    Button editButton;

    Button removeButton;

    AbstractObject currentlyEditing;

    public ViewProjectsPanel(ApplicationContext context, ProjectNavigator projectNavigator) {
        this.setSizeFull();
        this.context = context;
        this.projectNavigator = projectNavigator;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        saveButton = buttonBar.addButton("Save", FontAwesome.CHECK);
        saveButton.addClickListener(new SaveClickListener());
        saveButton.setVisible(false);

        cancelButton = buttonBar.addButton("Cancel", FontAwesome.TIMES);
        cancelButton.addClickListener(new CancelClickListener());
        cancelButton.setVisible(false);

        openProjectButton = buttonBar.addButton("Open Project", Icons.PROJECT);
        openProjectButton.addClickListener(new OpenProjectClickListener());

        newProjectButton = buttonBar.addButton("New Project", Icons.PROJECT);
        newProjectButton.addClickListener(new NewProjectClickListener());

        newVersionButton = buttonBar.addButton("New Version", Icons.VERSION);
        newVersionButton.addClickListener(new NewProjectVersionClickListener());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", Icons.DELETE);
        removeButton.addClickListener(new RemoveClickListener());

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
        treeTable.addValueChangeListener(new TreeTableValueChangeListener());
        treeTable.setTableFieldFactory(new FieldFactory());

        addComponent(treeTable);
        setExpandRatio(treeTable, 1);

        refresh();
    }

    protected void setButtonsEnabled() {
        Object obj = treeTable.getValue();
        removeButton.setEnabled(obj != null);
        openProjectButton.setEnabled(obj != null);
        editButton.setEnabled(obj != null);
        newVersionButton.setEnabled(obj instanceof Project);

        saveButton.setVisible(currentlyEditing != null);
        cancelButton.setVisible(currentlyEditing != null);

        removeButton.setVisible(currentlyEditing == null);
        openProjectButton.setVisible(currentlyEditing == null);
        editButton.setVisible(currentlyEditing == null);
        newProjectButton.setVisible(currentlyEditing == null);
        newVersionButton.setVisible(currentlyEditing == null);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void showing() {
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

        treeTable.setSortContainerPropertyId("name");
        treeTable.setSortAscending(true);
    }

    protected void add(Project project) {
        treeTable.addItem(new Object[] { project.getName(), project.getDescription(), null, null,
                project.getCreateTime() }, project);
        treeTable.setItemIcon(project, Icons.PROJECT);
        List<ProjectVersion> versions = project.getProjectVersions();
        for (ProjectVersion projectVersion : versions) {
            addToTable(project, projectVersion);
        }
    }

    protected void addToTable(Project project, ProjectVersion projectVersion) {
        treeTable.setChildrenAllowed(project, true);
        treeTable.addItem(new Object[] { projectVersion.getVersionLabel(), null, projectVersion.isLocked(),
                projectVersion.isArchived(), project.getCreateTime() }, projectVersion);
        treeTable.setItemIcon(projectVersion, Icons.VERSION);
        treeTable.setParent(projectVersion, project);
        treeTable.setChildrenAllowed(projectVersion, false);
    }

    class NewProjectVersionClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        }
    }

    class NewProjectClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Project project = new Project();
            project.setName("New Project");
            ProjectVersion version = new ProjectVersion();
            version.setName("1.0");
            version.setProject(project);
            project.getProjectVersions().add(version);
            IConfigurationService configurationService = context.getConfigurationService();
            configurationService.save(project);
            configurationService.save(version);
            add(project);
        }
    }

    class SaveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Item item = treeTable.getItem(currentlyEditing);
            if (item != null) {
                IConfigurationService configurationService = context.getConfigurationService();

                String name = (String) item.getItemProperty("name").getValue();
                if (currentlyEditing instanceof Project) {
                    Project project = (Project) currentlyEditing;
                    project.setName(name);
                    project.setDescription((String) item.getItemProperty("description").getValue());
                    configurationService.save(project);
                    projectNavigator.addProjectVersion(project.getLatestProjectVersion());

                } else if (currentlyEditing instanceof ProjectVersion) {
                    ProjectVersion version = (ProjectVersion) currentlyEditing;
                    version.setName(name);

                    Boolean locked = (Boolean) item.getItemProperty("locked").getValue();
                    locked = locked == null ? false : locked;
                    version.setLocked(locked);

                    Boolean archived = (Boolean) item.getItemProperty("archived").getValue();
                    archived = archived == null ? false : archived;
                    version.setArchived(archived);
                    configurationService.save(version);
                    projectNavigator.addProjectVersion(version);
                }

                currentlyEditing = null;
                setButtonsEnabled();
                treeTable.setSortContainerPropertyId(treeTable.getSortContainerPropertyId());
                treeTable.refreshRowCache();
            }
        }
    }

    class CancelClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            currentlyEditing = null;
            refresh();
            treeTable.refreshRowCache();
        }
    }

    class OpenProjectClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            openProject(treeTable.getValue());
        }

    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            if (currentlyEditing == null) {
                currentlyEditing = (AbstractObject) treeTable.getValue();
            }
            setButtonsEnabled();
            treeTable.refreshRowCache();
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
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

        }
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

    class TreeTableItemClickListener implements ItemClickListener {
        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                openProject(event.getItemId());
            }
        }
    }

    class TreeTableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            setButtonsEnabled();
            if (currentlyEditing != null) {
                currentlyEditing = null;
                treeTable.refreshRowCache();
                setButtonsEnabled();
            }
        }
    }

    class FieldFactory extends DefaultFieldFactory {
        @Override
        public Field<?> createField(Container container, Object itemId, Object propertyId,
                Component uiContext) {
            if (itemId.equals(currentlyEditing) && !propertyId.equals("createTime")) {
                Field<?> field = super.createField(container, itemId, propertyId, uiContext);
                if (field instanceof TextField) {
                    final TextField textField = (TextField) field;
                    textField.setNullRepresentation("");
                    textField.setWidth(100, Unit.PERCENTAGE);
                    textField.addValueChangeListener(new ValueChangeListener() {

                        @Override
                        public void valueChange(ValueChangeEvent event) {
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
