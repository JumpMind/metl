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
package org.jumpmind.metl.ui.views.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.components.grid.ItemClickListener;
import com.vaadin.ui.Grid;

@SuppressWarnings("serial")
@UiComponent
@Scope(value = "ui")
@Order(600)
@AdminMenuLink(name = "Plugin Repositories", id = "Plugin Repositories", icon = VaadinIcons.DATABASE)
public class PluginRepositoriesPanel extends AbstractAdminPanel {

    Button newButton;
    
    Button editButton;
    
    Button removeButton;
    
    List<PluginRepository> pluginRepositoryList = new ArrayList<PluginRepository>();
    
    Grid<PluginRepository> grid;
    
    public PluginRepositoriesPanel() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        newButton = buttonBar.addButton("Add", VaadinIcons.PLUS);
        newButton.addClickListener(new NewClickListener());

        editButton = buttonBar.addButton("Edit", VaadinIcons.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", VaadinIcons.TRASH);
        removeButton.addClickListener(new RemoveClickListener());

        grid = new Grid<PluginRepository>();
        grid.setSizeFull();
        //grid.setCacheRate(100);
        //grid.setPageLength(100);
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addColumn(PluginRepository::getName).setId("name").setCaption("Name").setSortable(true);
        grid.addColumn(PluginRepository::getUrl).setCaption("Url");
        grid.addColumn(PluginRepository::getLastUpdateTime).setCaption("Updated").setWidth(UIConstants.DATETIME_WIDTH_PIXELS);
        grid.addItemClickListener(new GridItemClickListener());
        grid.addSelectionListener(new GridSelectionListener());
        grid.sort("name", SortDirection.ASCENDING);

        addComponent(grid);
        setExpandRatio(grid, 1.0f);
    }

    @Override
    public void selected() {
        refresh();
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {        
    }

    public void refresh() {
        pluginRepositoryList.clear();
        pluginRepositoryList.addAll(context.getPluginService().findPluginRepositories());
        grid.setItems(pluginRepositoryList);
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        Set<PluginRepository> selectedIds = getSelectedItems();
        boolean enabled = selectedIds.size() > 0;
        editButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    protected Set<PluginRepository> getSelectedItems() {
        return grid.getSelectedItems();
    }

    protected PluginRepository getFirstSelectedItem() {
        Set<PluginRepository> pluginRepositorys = grid.getSelectedItems();
        Iterator<PluginRepository> iter = pluginRepositorys.iterator();
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    class NewClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            PluginRepository pluginRepository = new PluginRepository();
            PluginRepositoryEditPanel editPanel = new PluginRepositoryEditPanel(context, pluginRepository);
            adminView.getTabbedPanel().addCloseableTab(pluginRepository.getId(), "Edit Repository", getIcon(), editPanel);
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            PluginRepository pluginRepository = getFirstSelectedItem();
            context.getPluginService().refresh(pluginRepository);
            PluginRepositoryEditPanel editPanel = new PluginRepositoryEditPanel(context, pluginRepository);
            adminView.getTabbedPanel().addCloseableTab(pluginRepository.getId(), "Edit Repository", getIcon(), editPanel);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            for (PluginRepository pluginRepository : getSelectedItems()) {
                context.getConfigurationService().delete(pluginRepository);
                pluginRepositoryList.remove(pluginRepository);
            }
            
            grid.setItems(pluginRepositoryList);
            grid.deselectAll();
            setButtonsEnabled();
        }
    }

    class GridItemClickListener implements ItemClickListener<PluginRepository> {
        long lastClick;
        
        public void itemClick(ItemClick<PluginRepository> event) {
            if (event.getMouseEventDetails().isDoubleClick()) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItem()) &&
                System.currentTimeMillis()-lastClick > 500) {
                    grid.deselectAll();
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class GridSelectionListener implements SelectionListener<PluginRepository> {
        public void selectionChange(SelectionEvent<PluginRepository> event) {
            setButtonsEnabled();
        }
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }
}
