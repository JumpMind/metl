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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.core.model.ReleasePackage;
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
@Scope("ui")
@TopBarLink(id = "release", category = Category.Release, menuOrder = 20, name = "Release", icon = FontAwesome.CUBE)
public class ReleasesView extends VerticalLayout implements View {

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

    public ReleasesView() {
        setSizeFull();
        setMargin(false);

        ButtonBar buttonBar = new ButtonBar();
        addButton = buttonBar.addButton("Add", FontAwesome.PLUS, e -> add());
        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT, e -> add());
        exportButton = buttonBar.addButton("Export", FontAwesome.DOWNLOAD, e -> add());
        archiveButton = buttonBar.addButton("Archive", FontAwesome.ARCHIVE, e -> add());
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

    @PostConstruct
    protected void init() {
    }

    protected void refresh() {
        container.removeAllItems();
        try {
            container.addItem(new ReleasePackage("MDS", "6.0.0", new SimpleDateFormat("yyyy-MM-dd").parse("2016-07-15"), true));
            container.addItem(new ReleasePackage("MDS", "6.1.0", new SimpleDateFormat("yyyy-MM-dd").parse("2016-08-02"), true));
            container.addItem(new ReleasePackage("MDS", "7.0.0", new SimpleDateFormat("yyyy-MM-dd").parse("2017-05-01"), false));
        } catch (ParseException e) {
        }

    }

    protected void add() {

    }

    protected void edit() {

    }

    protected void archive() {

    }

    protected void export() {

    }

    protected void finalize() {

    }

    protected void rowSelected() {
        //Collection<Object> packages = grid.getSelectedRows();

    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

}
