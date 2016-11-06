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

import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionComponentPlugin;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.PostCommitHandler;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;

public class ProjectVersionPropertiesPanel extends Panel implements IUiPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    DesignNavigator designNavigator;

    ProjectVersion projectVersion;

    Grid componentPluginsGrid;

    BeanItemContainer<ProjectVersionComponentPlugin> componentPluginsGridContainer;
    
    Button updateButton;
    
    Button addButton;
    
    Button pinButton;
    
    Button unpinButton;

    public ProjectVersionPropertiesPanel(ProjectVersion projectVersion, ApplicationContext context, DesignNavigator projectNavigator) {
        this.setSizeFull();
        this.context = context;
        this.designNavigator = projectNavigator;
        this.projectVersion = projectVersion;
        
        VerticalLayout content = new VerticalLayout();
        setContent(content);
        
        HorizontalLayout versionPropertiesHeaderWrapper = new HorizontalLayout();
        versionPropertiesHeaderWrapper.setMargin(new MarginInfo(false, false, false, true));
        Label versionPropertiesHeader = new Label("Project Version Properties");
        versionPropertiesHeader.addStyleName(ValoTheme.LABEL_H3);
        versionPropertiesHeaderWrapper.addComponent(versionPropertiesHeader);
        content.addComponent(versionPropertiesHeaderWrapper);
        
        FormLayout formLayout = new FormLayout();
        CheckBox releasedCheckBox = new CheckBox("Released");
        releasedCheckBox.setImmediate(true);
        releasedCheckBox.setValue(projectVersion.isReleased());
        releasedCheckBox.addValueChangeListener(e->toggleReleased(e));
        formLayout.addComponent(releasedCheckBox);
        
        CheckBox archiveCheckBox = new CheckBox("Archived");
        archiveCheckBox.setImmediate(true);
        archiveCheckBox.setValue(projectVersion.isArchived());
        archiveCheckBox.addValueChangeListener(e->toggleArchived(e));
        formLayout.addComponent(archiveCheckBox);
        content.addComponent(formLayout);

        HorizontalLayout componentHeaderWrapper = new HorizontalLayout();
        componentHeaderWrapper.setMargin(new MarginInfo(false, false, false, true));
        Label componentHeader = new Label("Component Plugin Properties");
        componentHeader.addStyleName(ValoTheme.LABEL_H3);
        componentHeaderWrapper.addComponent(componentHeader);
        content.addComponent(componentHeaderWrapper);

        ButtonBar buttonBar = new ButtonBar();
        content.addComponent(buttonBar);
        addButton = buttonBar.addButton("Add", FontAwesome.PLUS, (event)->add());
        updateButton = buttonBar.addButton("Update", Icons.UPDATE, (event)->update());        
        pinButton =  buttonBar.addButton("Pin", FontAwesome.CHECK_CIRCLE_O, (event)->pin(true));
        unpinButton = buttonBar.addButton("Unpin", FontAwesome.CIRCLE_O, (event)->pin(false));

        componentPluginsGrid = new Grid();
        componentPluginsGrid.setSelectionMode(SelectionMode.MULTI);
        componentPluginsGrid.setHeightMode(HeightMode.ROW);
        componentPluginsGrid.setEditorEnabled(true);
        componentPluginsGrid.setWidth(100, Unit.PERCENTAGE);
        componentPluginsGrid.addColumn("componentTypeId", String.class).setHeaderCaption("Type").setEditable(false);
        componentPluginsGrid.addColumn("pluginId", String.class).setHeaderCaption("Plugin").setEditable(false);
        componentPluginsGrid.addColumn("enabled", Boolean.class).setHeaderCaption("Enabled").setWidth(75);
        componentPluginsGrid.addColumn("pinVersion", Boolean.class).setHeaderCaption("Pin Version").setWidth(95);
        final double VERSION_WIDTH = 190;
        componentPluginsGrid.addColumn("artifactVersion", String.class).setHeaderCaption("Version").setWidth(VERSION_WIDTH)
                .setEditable(false);
        componentPluginsGrid.addColumn("updatesAvailable", String.class).setHeaderCaption("").setWidth(55)
                .setEditable(false).setRenderer(new HtmlRenderer());
        componentPluginsGrid.addSelectionListener((event) -> setButtonsEnabled());
        
        componentPluginsGridContainer = new BeanItemContainer<>(ProjectVersionComponentPlugin.class);
        GeneratedPropertyContainer gpcontainer =
                new GeneratedPropertyContainer(componentPluginsGridContainer);
        gpcontainer.addGeneratedProperty("pluginId",
                new PropertyValueGenerator<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public String getValue(Item item, Object itemId,
                                        Object propertyId) {
                    ProjectVersionComponentPlugin plugin = (ProjectVersionComponentPlugin)itemId;
                    return String.format("%s:%s", plugin.getArtifactGroup(), plugin.getArtifactName());
                }

                @Override
                public Class<String> getType() {
                    return String.class;
                }
            });
        gpcontainer.addGeneratedProperty("updatesAvailable",
                new PropertyValueGenerator<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public String getValue(Item item, Object itemId,
                                        Object propertyId) {
                    ProjectVersionComponentPlugin plugin = (ProjectVersionComponentPlugin)itemId;
                    return !plugin.getArtifactVersion().equals(plugin.getLatestArtifactVersion()) ? "<span class='warn' title='Updates Available'>" + FontAwesome.WARNING.getHtml() + "</span>" : "";
                }

                @Override
                public Class<String> getType() {
                    return String.class;
                }
            });        
        componentPluginsGrid.setContainerDataSource(gpcontainer);
        
        componentPluginsGrid.getEditorFieldGroup().addCommitHandler(new PostCommitHandler(() -> {
            ProjectVersionComponentPlugin item = (ProjectVersionComponentPlugin) componentPluginsGrid.getEditedItemId();
            IConfigurationService configurationService = context.getConfigurationService();
            configurationService.save(item);
            componentPluginsGrid.markAsDirty();
        }));
        content.addComponent(componentPluginsGrid);

        VerticalLayout spacer = new VerticalLayout();
        content.addComponent(spacer);
        content.setExpandRatio(spacer, 1);

        refresh();
        
        populateContainer();
        
        setButtonsEnabled();        

    }
    
    protected void toggleReleased(ValueChangeEvent event) {
        Boolean value = (Boolean)event.getProperty().getValue();
        projectVersion.setReleased(value);
        context.getConfigurationService().save(projectVersion);
        designNavigator.refresh();
    }
    
    protected void toggleArchived(ValueChangeEvent event) {
        Boolean value = (Boolean)event.getProperty().getValue();
        projectVersion.setReleased(value);
        context.getConfigurationService().save(projectVersion);
        designNavigator.refresh();
    }
    
    protected void setButtonsEnabled() {
        Collection<Object> selectedRows = componentPluginsGrid.getSelectedRows();
        boolean updatesAvailable = false;
        for (Object object : selectedRows) {
            ProjectVersionComponentPlugin plugin = (ProjectVersionComponentPlugin)object;
            updatesAvailable |= plugin.isUpdateAvailable();
        }
        boolean selected = selectedRows.size() > 0;
        updateButton.setEnabled(updatesAvailable);        
        pinButton.setEnabled(selected);
        unpinButton.setEnabled(selected);
    }
    
    protected void refresh() {
        context.getComponentDefinitionFactory().refresh();
        populateContainer();
    }
    
    protected void add() {
        
    }
    
    protected void update() {
        Collection<Object> selectedRows = componentPluginsGrid.getSelectedRows();
        for (Object object : selectedRows) {
            ProjectVersionComponentPlugin plugin = (ProjectVersionComponentPlugin)object;
            if (plugin.isUpdateAvailable()) {
                plugin.setArtifactVersion(plugin.getLatestArtifactVersion());
                context.getConfigurationService().save(plugin);
            }            
        }    
        refresh();
        populateContainer();
        
    }
    
    protected void pin(boolean value) {
        Collection<Object> selectedRows = componentPluginsGrid.getSelectedRows();
        for (Object object : selectedRows) {
            ProjectVersionComponentPlugin plugin = (ProjectVersionComponentPlugin)object;
            plugin.setPinVersion(value);
            context.getConfigurationService().save(plugin);
        }        
        populateContainer();
    }
    
    protected void populateContainer() {
        componentPluginsGridContainer.removeAllItems();
        componentPluginsGridContainer.addAll(context.getConfigurationService().findProjectVersionComponentPlugins(projectVersion.getId()));
        componentPluginsGrid.setHeightByRows(componentPluginsGridContainer.size() > 0 ? componentPluginsGridContainer.size() : 1);
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
