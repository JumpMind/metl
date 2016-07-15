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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.FieldFactory;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.views.DesignNavigator;
import org.jumpmind.metl.ui.views.UiConstants;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.NotifyDialog;
import org.jumpmind.vaadin.ui.common.PromptDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitEvent;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.sort.SortOrder;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.converter.StringToBooleanConverter;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;

public class ManageProjectsPanel extends VerticalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    final static float ROW_PIXELS = 31;

    ApplicationContext context;

    DesignNavigator projectNavigator;

    Grid projectGrid;

    BeanItemContainer<Project> gridContainer;

    Button newProjectButton;

    Button editButton;

    Button removeButton;

    List<SortOrder> lastSortOrder;

    public ManageProjectsPanel(ApplicationContext context, DesignNavigator projectNavigator) {
        this.setSizeFull();
        this.context = context;
        this.projectNavigator = projectNavigator;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        newProjectButton = buttonBar.addButton("New Project", Icons.PROJECT);
        newProjectButton.addClickListener(event -> newProject());

        editButton = buttonBar.addButton("Edit Project", FontAwesome.EDIT);
        editButton.addClickListener(event -> edit(projectGrid));

        removeButton = buttonBar.addButton("Remove Project", Icons.DELETE);
        removeButton.addClickListener(event -> removeProject());

        gridContainer = new BeanItemContainer<>(Project.class);
        projectGrid = new Grid();
        projectGrid.setSizeFull();
        projectGrid.setEditorEnabled(true);
        projectGrid.setSelectionMode(SelectionMode.SINGLE);

        projectGrid.addColumn("name", String.class).setHeaderCaption("Name").setExpandRatio(2);
        projectGrid.addColumn("description", String.class).setHeaderCaption("Description")
                .setExpandRatio(1);
        projectGrid.addColumn("createTime", Date.class).setHeaderCaption("Create Time")
                .setWidth(185).setMaximumWidth(200)
                .setRenderer(new DateRenderer(UiConstants.DATETIME_FORMAT)).setEditable(false);

        projectGrid.setContainerDataSource(gridContainer);
        projectGrid.setEditorFieldFactory(new FieldFactory());
        projectGrid.addSortListener(event -> {
            lastSortOrder = event.getSortOrder();
        });
        projectGrid.addSelectionListener(event -> setButtonsEnabled());
        projectGrid.addItemClickListener(new GridClickListener(projectGrid));
        projectGrid.addSelectionListener((event) -> {
            Set<Object> removed = event.getRemoved();
            for (Object remove : removed) {
                projectGrid.setDetailsVisible(remove, false);
            }
            Set<Object> selected = event.getSelected();
            for (Object select : selected) {
                projectGrid.setDetailsVisible(select, true);
            }

        });

        projectGrid.getEditorFieldGroup().addCommitHandler(new FieldGroup.CommitHandler() {

            private static final long serialVersionUID = 1L;

            @Override
            public void preCommit(CommitEvent commitEvent) throws CommitException {
            }

            @Override
            public void postCommit(CommitEvent commitEvent) throws CommitException {
                Project item = (Project) projectGrid.getEditedItemId();
                IConfigurationService configurationService = context.getConfigurationService();
                configurationService.save(item);
                projectGrid.markAsDirty();
            }
        });

        HeaderRow filteringHeader = projectGrid.appendHeaderRow();
        HeaderCell logTextFilterCell = filteringHeader.getCell("name");
        TextField filterField = new TextField();
        filterField.setInputPrompt("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        filterField.setWidth("100%");

        filterField.addTextChangeListener(change -> {
            gridContainer.removeContainerFilters("name");
            if (!change.getText().isEmpty()) {
                gridContainer.addContainerFilter(
                        new SimpleStringFilter("name", change.getText(), true, false));
            }
        });
        logTextFilterCell.setComponent(filterField);

        projectGrid.setDetailsGenerator(
                (rowReference) -> buildVersionGrid((Project) rowReference.getItemId()));

        addComponent(projectGrid);
        setExpandRatio(projectGrid, 1);

        setButtonsEnabled();

    }

    protected Component buildVersionGrid(Project project) {
        context.getConfigurationService().refresh(project);
        List<ProjectVersion> versions = project.getProjectVersions();
        BeanItemContainer<ProjectVersion> versionGridContainer = new BeanItemContainer<>(
                ProjectVersion.class);
        Grid versionGrid = new Grid();

        final VerticalLayout layout = new VerticalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setHeight(100 + (versions.size() * ROW_PIXELS), Unit.PIXELS);
        layout.setMargin(true);
        layout.setSpacing(true);
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        Button openButton = new Button("Open Version", (event) -> {
            Collection<Object> selected = versionGrid.getSelectedRows();
            for (Object object : selected) {
                projectNavigator.addProjectVersion(((ProjectVersion) object));
            }
        });
        buttons.addComponent(openButton);
        Button newButton = new Button("New Version", (event) -> newVersion(layout, versionGrid));
        buttons.addComponent(newButton);
        Button editButton = new Button("Edit Version", (event) -> edit(versionGrid));
        buttons.addComponent(editButton);
        Button removeButton = new Button("Remove Version",
                (event) -> removeVersion(layout, versionGrid));
        buttons.addComponent(removeButton);

        openButton.setEnabled(false);
        newButton.setEnabled(false);
        removeButton.setEnabled(false);
        editButton.setEnabled(false);

        layout.addComponent(buttons);

        versionGrid.setHeightMode(HeightMode.ROW);
        versionGrid.setHeightByRows(versions.size());
        versionGrid.setWidth(100, Unit.PERCENTAGE);
        versionGrid.setEditorEnabled(true);
        versionGrid.setSelectionMode(SelectionMode.SINGLE);

        versionGrid.addColumn("versionLabel", String.class).setHeaderCaption("Version")
                .setExpandRatio(2);
        versionGrid.addColumn("description", String.class).setHeaderCaption("Description")
                .setExpandRatio(1);
        versionGrid.addColumn("readOnly", Boolean.class).setHeaderCaption("Read Only")
                .setMaximumWidth(100)
                .setRenderer(new HtmlRenderer(), new StringToBooleanConverter() {
                    private static final long serialVersionUID = 1L;

                    protected String getTrueString() {
                        return FontAwesome.CHECK.getHtml();
                    };

                    protected String getFalseString() {
                        return "";
                    };
                });
        versionGrid.addColumn("createTime", Date.class).setHeaderCaption("Create Time")
                .setWidth(185).setMaximumWidth(200)
                .setRenderer(new DateRenderer(UiConstants.DATETIME_FORMAT)).setEditable(false);

        versionGrid.setContainerDataSource(versionGridContainer);
        versionGrid.setEditorFieldFactory(new FieldFactory());

        versionGrid.getEditorFieldGroup().addCommitHandler(new FieldGroup.CommitHandler() {

            private static final long serialVersionUID = 1L;

            @Override
            public void preCommit(CommitEvent commitEvent) throws CommitException {
            }

            @Override
            public void postCommit(CommitEvent commitEvent) throws CommitException {
                ProjectVersion item = (ProjectVersion) versionGrid.getEditedItemId();
                IConfigurationService configurationService = context.getConfigurationService();
                configurationService.save(item);
                projectGrid.markAsDirty();
            }
        });

        versionGrid.addSelectionListener((event) -> {
            int numberSelected = versionGrid.getSelectedRows().size();
            boolean currentlyEditing = projectGrid.getEditedItemId() != null;
            boolean selected = numberSelected > 0 && !currentlyEditing;
            openButton.setEnabled(selected);
            newButton.setEnabled(selected);
            removeButton.setEnabled(selected);
            editButton.setEnabled(selected);
        });
        versionGrid.addItemClickListener(new GridClickListener(versionGrid));
        layout.addComponent(versionGrid);
        layout.setExpandRatio(versionGrid, 1);
        layout.addComponent(new Label(" "));
        versionGridContainer.addAll(versions);
        versionGrid.sort("versionLabel", SortDirection.DESCENDING);

        return layout;
    }

    protected void sort() {
        if (lastSortOrder == null) {
            lastSortOrder = new ArrayList<>();
            lastSortOrder.add(new SortOrder("name", SortDirection.ASCENDING));
        }
        projectGrid.setSortOrder(lastSortOrder);
    }

    protected void deselectAll() {
        Collection<Object> all = projectGrid.getSelectedRows();
        for (Object selected : all) {
            projectGrid.deselect(selected);
        }
    }

    protected void setButtonsEnabled() {
        int numberSelected = projectGrid.getSelectionModel().getSelectedRows().size();
        boolean selected = numberSelected > 0;
        removeButton.setEnabled(selected);
        editButton.setEnabled(selected);

        boolean currentlyEditing = projectGrid.getEditedItemId() != null;
        if (currentlyEditing) {
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
            newProjectButton.setEnabled(false);
        }
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
        refresh();
    }

    @Override
    public void deselected() {
    }

    protected void refresh() {
        Collection<Object> selected = projectGrid.getSelectedRows();
        gridContainer.removeAllItems();
        IConfigurationService configurationService = context.getConfigurationService();

        List<Project> projects = configurationService.findProjects();
        for (Project project : projects) {
            projectGrid.getContainerDataSource().addItem(project);
        }

        for (Object s : selected) {
            if (projectGrid.getContainerDataSource().containsId(s)) {
                projectGrid.select(s);
            }
        }

        setButtonsEnabled();

        sort();

    }

    protected void edit(Grid grid) {
        Collection<Object> selected = grid.getSelectedRows();
        if (selected.size() > 0) {
            grid.editItem(selected.iterator().next());
        }
    }

    protected void newVersion(VerticalLayout layout, Grid grid) {
        Collection<Object> selected = grid.getSelectedRows();
        if (selected.size() == 1) {
            IConfigurationService configurationService = context.getConfigurationService();
            ProjectVersion originalVersion = (ProjectVersion) selected.iterator().next();
            PromptDialog.prompt("New Version",
                    String.format(
                            "Copying version '%s' of '%s'. What do you want to name new version?",
                            originalVersion.getVersionLabel(),
                            originalVersion.getProject().getName()),
                    (newVersionLabel) -> {
                        if (StringUtils.isNotBlank(newVersionLabel)) {
                            ProjectVersion newVersion = configurationService
                                    .saveNewVersion(newVersionLabel, originalVersion);
                            Indexed indexed = grid.getContainerDataSource();
                            indexed.addItemAfter(originalVersion, newVersion);
                            this.projectGrid.deselect(originalVersion.getProject());
                            this.projectGrid.select(originalVersion.getProject());
                            return true;
                        } else {
                            NotifyDialog.show("Please specify a version number",
                                    "Please specify a version number", null, Type.WARNING_MESSAGE);
                            return false;
                        }
                    });
        }

    }

    protected void newProject() {
        deselectAll();
        Project project = new Project();
        project.setName("New Project");
        ProjectVersion version = new ProjectVersion();
        version.setVersionLabel("1.0.0");
        version.setProject(project);
        project.getProjectVersions().add(version);
        IConfigurationService configurationService = context.getConfigurationService();
        configurationService.save(project);
        configurationService.save(version);
        projectGrid.getContainerDataSource().addItem(project);
        projectGrid.select(project);
        projectGrid.editItem(project);
    }

    protected void removeVersion(VerticalLayout layout, Grid grid) {
        ConfirmDialog.show("Delete Version(s)?",
                "Are you sure you want to delete the selected version(s)?", () -> {
                    ProjectVersion item = (ProjectVersion) grid.getSelectedRow();
                    grid.getContainerDataSource().removeItem(item);
                    item.setDeleted(true);
                    context.getConfigurationService().save(item);
                    sort();
                    setButtonsEnabled();
                    this.projectGrid.deselect(item.getProject());
                    this.projectGrid.select(item.getProject());
                    return true;
                });
    }

    protected void removeProject() {
        ConfirmDialog.show("Delete Project(s)?",
                "Are you sure you want to delete the selected project(s)?", () -> {
                    Collection<Object> selected = projectGrid.getSelectedRows();
                    for (Object object : selected) {
                        Project item = (Project) object;
                        projectGrid.getContainerDataSource().removeItem(item);
                        item.setDeleted(true);
                        List<ProjectVersion> versions = item.getProjectVersions();
                        for (ProjectVersion version : versions) {
                            version.setDeleted(true);
                            context.getConfigurationService().save(version);
                        }
                        context.getConfigurationService().save(item);
                    }
                    sort();
                    setButtonsEnabled();
                    return true;
                });
    }

    class GridClickListener implements ItemClickListener {
        private static final long serialVersionUID = 1L;
        Grid grid;

        public GridClickListener(Grid grid) {
            this.grid = grid;
        }

        @Override
        public void itemClick(ItemClickEvent event) {
            if (!event.isDoubleClick()) {
                if (!event.isShiftKey()) {
                    Collection<Object> all = grid.getSelectedRows();
                    for (Object selected : all) {
                        if (!selected.equals(event.getItemId())) {
                            grid.deselect(selected);
                        }
                    }
                }
                if (grid.getSelectedRows().contains(event.getItemId())) {
                    grid.deselect(event.getItemId());
                } else {
                    grid.select(event.getItemId());
                }
            }
        }
    }

}
