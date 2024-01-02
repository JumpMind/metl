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

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;

public class ProjectVersionSettingsPanel extends Panel implements IUiPanel {

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
        this.context = context;
        this.designNavigator = projectNavigator;
        this.projectVersion = projectVersion;
        
        VerticalLayout content = new VerticalLayout();
        setContent(content);
        
        addHeader("Project Version Settings");
        
        FormLayout formLayout = new FormLayout();
        formLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
        DateField releaseDateField = new DateField("Release Date");
        Date releaseDate = projectVersion.getReleaseDate();
        if (releaseDate != null) {
        	releaseDateField.setValue(releaseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        releaseDateField.setEnabled(false);        
        formLayout.addComponent(releaseDateField);
        
        CheckBox archiveCheckBox = new CheckBox("Archived");
        archiveCheckBox.setValue(projectVersion.isArchived());
        archiveCheckBox.addValueChangeListener(e->toggleArchived(e));
        formLayout.addComponent(archiveCheckBox);
        content.addComponent(formLayout);

        addHeader("Component Plugin Settings");

        ButtonBar buttonBar = new ButtonBar();
        content.addComponent(buttonBar);
        buttonBar.addButton("Refresh", Icons.REFRESH, (event)->refreshPlugins()); 
        updateButton = buttonBar.addButton("Update", Icons.UPDATE, (event)->update());        
        pinButton =  buttonBar.addButton("Pin", VaadinIcons.CHECK_CIRCLE_O, (event)->pin(true));
        unpinButton = buttonBar.addButton("Unpin", VaadinIcons.CIRCLE_THIN, (event)->pin(false));

        componentPluginsGrid = new Grid<ProjectVersionPlugin>();
        componentPluginsGrid.setSelectionMode(SelectionMode.MULTI);
        componentPluginsGrid.setHeightMode(HeightMode.ROW);
        componentPluginsGrid.setWidth(100, Unit.PERCENTAGE);
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getDefinitionType).setCaption("Plugin Type");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getDefinitionName).setCaption("Name");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getDefinitionTypeId).setCaption("Type");
        componentPluginsGrid.addColumn(plugin -> String.format("%s:%s", plugin.getArtifactGroup(), plugin.getArtifactName())).setCaption("Plugin");
        componentPluginsGrid.addColumn(ProjectVersionPlugin::isEnabled).setCaption("Enabled").setWidth(75);
        componentPluginsGrid.addColumn(ProjectVersionPlugin::isPinVersion).setCaption("Pin Version").setWidth(95);
        
        final double VERSION_WIDTH = 190;
        
        componentPluginsGrid.addColumn(ProjectVersionPlugin::getArtifactVersion).setCaption("Version").setWidth(VERSION_WIDTH);
        componentPluginsGrid.addColumn(plugin -> {
        	return !plugin.getArtifactVersion().equals(plugin.getLatestArtifactVersion()) ? "<span class='warn' title='Updates Available'>" + VaadinIcons.WARNING.getHtml() + "</span>" : "";
        }).setCaption("").setWidth(55).setRenderer(new HtmlRenderer());
        componentPluginsGrid.addSelectionListener((event) -> setButtonsEnabled());
        
        content.addComponent(componentPluginsGrid);

        VerticalLayout spacer = new VerticalLayout();
        content.addComponent(spacer);
        content.setExpandRatio(spacer, 1);

        refresh();        
        setButtonsEnabled();        

    }
    
    protected void refreshPlugins() {
        context.getDefinitionFactory().refresh(projectVersion.getId());
        populateContainer();
    }
    
    protected void addHeader(String caption) {
        HorizontalLayout componentHeaderWrapper = new HorizontalLayout();
        componentHeaderWrapper.setMargin(new MarginInfo(false, false, false, true));
        Label componentHeader = new Label(caption);
        componentHeader.addStyleName(ValoTheme.LABEL_H3);
        componentHeader.addStyleName(ValoTheme.LABEL_COLORED);
        componentHeaderWrapper.addComponent(componentHeader);
        ((AbstractLayout)getContent()).addComponent(componentHeaderWrapper);
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
        componentPluginsGrid.setHeightByRows(plugins.size() > 0 ? plugins.size() : 1);
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
