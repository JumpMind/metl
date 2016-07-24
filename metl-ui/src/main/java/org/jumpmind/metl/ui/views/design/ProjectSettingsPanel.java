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

import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionComponentPlugin;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.PostCommitHandler;
import org.jumpmind.metl.ui.views.DesignNavigator;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ProjectSettingsPanel extends VerticalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    DesignNavigator projectNavigator;

    ProjectVersion projectVersion;

    Grid componentPluginsGrid;

    BeanItemContainer<ProjectVersionComponentPlugin> componentPluginsGridContainer;

    public ProjectSettingsPanel(ProjectVersion projectVersion, ApplicationContext context, DesignNavigator projectNavigator) {
        this.setSizeFull();
        this.context = context;
        this.projectNavigator = projectNavigator;
        this.projectVersion = projectVersion;

        HorizontalLayout componentHeaderWrapper = new HorizontalLayout();
        componentHeaderWrapper.setMargin(new MarginInfo(false, false, false, true));
        Label componentHeader = new Label("Component Plugin Settings");
        componentHeader.addStyleName(ValoTheme.LABEL_H3);
        componentHeaderWrapper.addComponent(componentHeader);
        addComponent(componentHeaderWrapper);

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
        buttonBar.addButton("Refresh", Icons.REFRESH);
        buttonBar.addButton("Update", Icons.UPDATE);
        buttonBar.addButton("Upload", Icons.UPLOAD);

        componentPluginsGrid = new Grid();
        componentPluginsGrid.setSelectionMode(SelectionMode.MULTI);
        componentPluginsGrid.setHeightMode(HeightMode.ROW);
        componentPluginsGrid.setEditorEnabled(true);
        componentPluginsGrid.setWidth(100, Unit.PERCENTAGE);
        componentPluginsGrid.addColumn("componentTypeId", String.class).setHeaderCaption("Type").setEditable(false);
        componentPluginsGrid.addColumn("enabled", Boolean.class).setHeaderCaption("Enabled").setWidth(75);
        componentPluginsGrid.addColumn("pinVersion", Boolean.class).setHeaderCaption("Pin Version").setWidth(95);
        final double VERSION_WIDTH = 190;
        componentPluginsGrid.addColumn("artifactVersion", String.class).setHeaderCaption("Version").setWidth(VERSION_WIDTH)
                .setEditable(false);
        componentPluginsGrid.addColumn("latestArtifactVersion", String.class).setHeaderCaption("Latest Version").setWidth(VERSION_WIDTH)
                .setEditable(false);
        componentPluginsGridContainer = new BeanItemContainer<>(ProjectVersionComponentPlugin.class);
        componentPluginsGrid.setContainerDataSource(componentPluginsGridContainer);
        componentPluginsGrid.getEditorFieldGroup().addCommitHandler(new PostCommitHandler(() -> {
            ProjectVersionComponentPlugin item = (ProjectVersionComponentPlugin) componentPluginsGrid.getEditedItemId();
            IConfigurationService configurationService = context.getConfigurationService();
            configurationService.save(item);
            componentPluginsGrid.markAsDirty();
        }));
        addComponent(componentPluginsGrid);

        VerticalLayout spacer = new VerticalLayout();
        addComponent(spacer);
        setExpandRatio(spacer, 1);

        populateContainer();

    }

    protected void populateContainer() {
        componentPluginsGridContainer.removeAllItems();
        componentPluginsGridContainer.addAll(context.getConfigurationService().findProjectVersionComponentPlugin(projectVersion.getId()));
        componentPluginsGrid.setHeightByRows(componentPluginsGridContainer.size());
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
