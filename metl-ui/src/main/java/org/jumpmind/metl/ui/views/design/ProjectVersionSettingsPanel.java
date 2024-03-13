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

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionPlugin;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ProjectVersionSettingsPanel extends VerticalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    DesignNavigator designNavigator;

    ProjectVersion projectVersion;

    Grid<ProjectVersionPlugin> componentPluginsGrid;
    
    Button updateButton;
    
    Button pinButton;
    
    Button unpinButton;

    public ProjectVersionSettingsPanel(ProjectVersion projectVersion, ApplicationContext context, DesignNavigator projectNavigator) {
        this.setSizeFull();
        this.setPadding(false);
        this.setSpacing(false);
        this.context = context;
        this.designNavigator = projectNavigator;
        this.projectVersion = projectVersion;
        
        H3 projectVersionHeader = new H3("Project Version Settings");
        projectVersionHeader.getStyle().set("padding", "16px 0 0 8px");
        add(projectVersionHeader);
        
        FormLayout formLayout = new FormLayout();
        formLayout.getStyle().set("padding-left", "8px");
        formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
        DatePicker releaseDateField = new DatePicker();
        Date releaseDate = projectVersion.getReleaseDate();
        if (releaseDate != null) {
        	releaseDateField.setValue(releaseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        releaseDateField.setEnabled(false);        
        formLayout.addFormItem(releaseDateField, "Release Date");
        
        Checkbox archiveCheckbox = new Checkbox("Archived");
        archiveCheckbox.setValue(projectVersion.isArchived());
        archiveCheckbox.addValueChangeListener(e->toggleArchived(e));
        formLayout.addFormItem(archiveCheckbox, "");
        add(formLayout);

        H3 componentPluginHeader = new H3("Component Plugin Settings");
        componentPluginHeader.getStyle().set("padding", "16px 0 0 8px");
        add(componentPluginHeader);
        
        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);
        buttonBar.addButton("Refresh", Icons.REFRESH, (event)->refreshPlugins()); 
        updateButton = buttonBar.addButton("Update", Icons.UPDATE, (event)->update());        
        pinButton =  buttonBar.addButton("Pin", VaadinIcon.CHECK_CIRCLE_O, (event)->pin(true));
        unpinButton = buttonBar.addButton("Unpin", VaadinIcon.CIRCLE_THIN, (event)->pin(false));

        componentPluginsGrid = new Grid<ProjectVersionPlugin>();
        componentPluginsGrid.setSelectionMode(SelectionMode.MULTI);
        componentPluginsGrid.setAllRowsVisible(true);
        componentPluginsGrid.setWidthFull();
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getDefinitionType).setHeader("Plugin Type");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getDefinitionName).setHeader("Name");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getDefinitionTypeId).setHeader("Type");
        componentPluginsGrid.addColumn(plugin -> String.format("%s:%s", plugin.getArtifactGroup(), plugin.getArtifactName())).setHeader("Plugin");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::isEnabled).setHeader("Enabled").setFlexGrow(0).setWidth("85px");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::isPinVersion).setHeader("Pin Version").setFlexGrow(0).setWidth("105px");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getArtifactVersion).setHeader("Version").setFlexGrow(0).setWidth("190px");
        componentPluginsGrid.addComponentColumn(plugin -> {
            if (plugin.getArtifactVersion().equals(plugin.getLatestArtifactVersion())) {
                return null;
            }
            Icon icon = new Icon(VaadinIcon.WARNING);
            icon.addClassName("warn");
            icon.getElement().setProperty("title", "Updates Available");
            return icon;
        }).setHeader("").setWidth("55px");
        componentPluginsGrid.addSelectionListener((event) -> setButtonsEnabled());
        
        add(componentPluginsGrid);

        VerticalLayout spacer = new VerticalLayout();
        addAndExpand(spacer);

        refresh();        
        setButtonsEnabled();        

    }
    
    protected void refreshPlugins() {
        context.getDefinitionFactory().refresh(projectVersion.getId());
        populateContainer();
    }
    
    protected void addHeader(String caption) {
        HorizontalLayout componentHeaderWrapper = new HorizontalLayout();
        componentHeaderWrapper.getStyle().set("margin", "0 0 0 16px");
        H3 componentHeader = new H3(caption);
        componentHeaderWrapper.add(componentHeader);
        add(componentHeaderWrapper);
    }
    
    protected void toggleArchived(ValueChangeEvent<Boolean> event) {
        context.getConfigurationService().save(projectVersion);
        designNavigator.refresh();
    }
    
    protected void setButtonsEnabled() {
        boolean readOnly = context.isReadOnly(projectVersion, Privilege.DESIGN);
        Set<ProjectVersionPlugin> selectedRows = componentPluginsGrid.getSelectedItems();
        boolean updatesAvailable = false;
        for (ProjectVersionPlugin plugin : selectedRows) {
            updatesAvailable |= plugin.isUpdateAvailable();
        }
        boolean selected = selectedRows.size() > 0 && !readOnly;
        updateButton.setEnabled(updatesAvailable && !readOnly);        
        pinButton.setEnabled(selected);
        unpinButton.setEnabled(selected);
    }
    
    protected void refresh() {
        populateContainer();
    }
    
    protected void update() {
        Set<ProjectVersionPlugin> selectedRows = componentPluginsGrid.getSelectedItems();
        for (ProjectVersionPlugin plugin : selectedRows) {
            if (plugin.isUpdateAvailable()) {
                plugin.setArtifactVersion(plugin.getLatestArtifactVersion());
                context.getConfigurationService().save(plugin);
            }            
        }    
        refresh();
        populateContainer();
        setButtonsEnabled();        
    }
    
    protected void pin(boolean value) {
    	Set<ProjectVersionPlugin> selectedRows = componentPluginsGrid.getSelectedItems();
        for (ProjectVersionPlugin plugin : selectedRows) {
            plugin.setPinVersion(value);
            context.getConfigurationService().save(plugin);
        }        
        populateContainer();
    }
    
    protected void populateContainer() {
        IConfigurationService configurationService = context.getConfigurationService();
        List<ProjectVersionPlugin> plugins = configurationService.findProjectVersionComponentPlugins(projectVersion.getId());
        componentPluginsGrid.setItems(plugins);
    }

    public ProjectVersion getProjectVersion() {
        return projectVersion;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    @Override
    public boolean closing() {
        return true;
    }

}
