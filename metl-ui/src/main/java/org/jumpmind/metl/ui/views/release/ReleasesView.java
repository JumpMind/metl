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
package org.jumpmind.metl.ui.views.release;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.VerticalLayout;

@UiComponent
@Scope(value = "ui")
@TopBarLink(id = "release", category = Category.Release, menuOrder = 20, name = "Release", icon = FontAwesome.CUBE)
public class ReleasesView extends VerticalLayout implements View, IReleasePackageListener {

    private static final long serialVersionUID = 1L;

    protected static final Logger log = LoggerFactory.getLogger(ReleasesView.class);

    @Autowired
    ApplicationContext context;

    Button addButton;

    Button editButton;

    Button exportButton;

    Button archiveButton;

    Button finalizeButton;

    Grid grid;

    BeanItemContainer<ReleasePackage> container;
    
    IConfigurationService configService;

    public ReleasesView() {
        setSizeFull();
        setMargin(false);

        ButtonBar buttonBar = new ButtonBar();
        addButton = buttonBar.addButton("Add", FontAwesome.PLUS, e -> add());
        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT, e -> edit());
        exportButton = buttonBar.addButton("Export", FontAwesome.DOWNLOAD, e -> export());
        archiveButton = buttonBar.addButton("Archive", FontAwesome.ARCHIVE, e -> archive());
        finalizeButton = buttonBar.addButton("Finalize", FontAwesome.CUBE, e -> finalize());

        addComponent(buttonBar);

        grid = new Grid();
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.addSelectionListener((e) -> rowSelected());
        container = new BeanItemContainer<>(ReleasePackage.class);
        grid.setContainerDataSource(container);
        grid.setColumns("name", "version", "releaseDate", "released");
        addComponent(grid);
        setExpandRatio(grid, 1);
    }

    @Override
    public void updated(ReleasePackage releasePackage) {
    }

    @PostConstruct
    protected void init() {
        configService = context.getConfigurationService();
    }

    protected void refresh() {
        container.removeAllItems();
        List<ReleasePackage> releasePackages = configService.findReleasePackages();
        container.addAll(releasePackages);
    }

    protected void add() {
        new EditReleasePackageDialog(null, context, this).show();
    }

    protected void edit() {
        new EditReleasePackageDialog(getFirstSelectedReleasePackage(), context, this).show();
    }
    
    protected ReleasePackage getFirstSelectedReleasePackage() {
        Collection<Object> collection = grid.getSelectedRows();
        if (collection.size() > 0) {
            return (ReleasePackage)collection.iterator().next();
        } else {
            return null;
        }
    }

    protected void archive() {

    }

    protected void export() {

    }

    protected void finalize() {
        // TODO prompt for confirmation,  call service methods to rename master and create a new master
    }

    protected void rowSelected() {
        // Collection<Object> packages = grid.getSelectedRows();
        // TODO enable or diable buttons

    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

}
