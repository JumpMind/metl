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
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.spring.annotation.UIScope;

@SuppressWarnings("serial")
@UiComponent
@UIScope
@Order(600)
@AdminMenuLink(name = "Plugin Repositories", id = "Plugin Repositories", icon = VaadinIcon.DATABASE)
public class PluginRepositoriesPanel extends AbstractAdminPanel {

    Button newButton;
    
    Button editButton;
    
    Button removeButton;
    
    List<PluginRepository> pluginRepositoryList = new ArrayList<PluginRepository>();
    
    Grid<PluginRepository> grid;
    
    public PluginRepositoriesPanel() {
        setPadding(false);
        setSpacing(false);
        
        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);

        newButton = buttonBar.addButton("Add", VaadinIcon.PLUS);
        newButton.addClickListener(new NewClickListener());

        editButton = buttonBar.addButton("Edit", VaadinIcon.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", VaadinIcon.TRASH);
        removeButton.addClickListener(new RemoveClickListener());

        grid = new Grid<PluginRepository>();
        grid.setSizeFull();
        grid.setPageSize(100);
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addColumn(PluginRepository::getName).setKey("name").setHeader("Name").setFlexGrow(1).setSortable(true);
        grid.addColumn(PluginRepository::getUrl).setHeader("Url").setFlexGrow(5);
        grid.addColumn(PluginRepository::getLastUpdateTime).setHeader("Updated").setFlexGrow(0).setWidth("250px");
        grid.addItemClickListener(new GridItemClickListener());
        grid.addSelectionListener(new GridSelectionListener());
        List<GridSortOrder<PluginRepository>> orderList = new ArrayList<GridSortOrder<PluginRepository>>();
        orderList.add(new GridSortOrder<PluginRepository>(grid.getColumnByKey("name"), SortDirection.ASCENDING));
        grid.sort(orderList);

        add(grid);
        expand(grid);
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

    class NewClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            PluginRepository pluginRepository = new PluginRepository();
            PluginRepositoryEditPanel editPanel = new PluginRepositoryEditPanel(context, pluginRepository, () -> refresh());
            adminView.getTabbedPanel().addCloseableTab(pluginRepository.getId(), "Edit Repository", new Icon(VaadinIcon.DATABASE), editPanel);
        }
    }

    class EditClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            PluginRepository pluginRepository = getFirstSelectedItem();
            context.getPluginService().refresh(pluginRepository);
            PluginRepositoryEditPanel editPanel = new PluginRepositoryEditPanel(context, pluginRepository, () -> refresh());
            adminView.getTabbedPanel().addCloseableTab(pluginRepository.getId(), "Edit Repository", new Icon(VaadinIcon.DATABASE), editPanel);
        }
    }

    class RemoveClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            for (PluginRepository pluginRepository : getSelectedItems()) {
                context.getConfigurationService().delete(pluginRepository);
                pluginRepositoryList.remove(pluginRepository);
            }
            
            grid.setItems(pluginRepositoryList);
            grid.deselectAll();
            setButtonsEnabled();
        }
    }

    class GridItemClickListener implements ComponentEventListener<ItemClickEvent<PluginRepository>> {
        long lastClick;
        
        public void onComponentEvent(ItemClickEvent<PluginRepository> event) {
            if (event.getClickCount() == 2) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItem()) &&
                System.currentTimeMillis()-lastClick > 500) {
                    grid.deselectAll();
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class GridSelectionListener implements SelectionListener<Grid<PluginRepository>, PluginRepository> {
        public void selectionChange(SelectionEvent<Grid<PluginRepository>, PluginRepository> event) {
            setButtonsEnabled();
        }
    }
}
