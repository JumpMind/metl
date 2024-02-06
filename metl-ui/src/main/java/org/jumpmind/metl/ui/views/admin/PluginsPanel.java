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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.persist.IPluginService;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;

@SuppressWarnings("serial")
@UiComponent
@Scope(value = "ui")
@Order(700)
@AdminMenuLink(name = "Plugins", id = "Plugins", icon = VaadinIcon.PUZZLE_PIECE)
public class PluginsPanel extends AbstractAdminPanel {

    Button addButton;

    Button removeButton;

    Button moveUpButton;

    Button moveDownButton;

    Grid<Plugin> grid;

    List<Plugin> plugins;

    public PluginsPanel() {
    }
    
    @PostConstruct
    @Override
    public void init() {
        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);

        addButton = buttonBar.addButton("Add", VaadinIcon.PLUS);
        addButton.addClickListener(e -> addPlugin());

        moveUpButton = buttonBar.addButton("Move Up", VaadinIcon.ARROW_UP, e -> moveUp());

        moveDownButton = buttonBar.addButton("Move Down", VaadinIcon.ARROW_DOWN, e -> moveDown());

        removeButton = buttonBar.addButton("Purge Unused", VaadinIcon.TRASH, e -> purgeUnused());

        grid = new Grid<Plugin>();
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addColumn(Plugin::getArtifactGroup).setHeader("Group").setSortable(false);
        grid.addColumn(Plugin::getArtifactName).setHeader("Name").setSortable(false);
        grid.addColumn(Plugin::getArtifactVersion).setHeader("Version").setSortable(false);
        grid.addColumn(Plugin::getLastUpdateTime).setHeader("Updated").setWidth("165px").setSortable(false);
        grid.addSelectionListener(e -> setButtonsEnabled());

        add(grid);
        expand(grid);

        context.getPluginManager().refresh();
    }

    public List<Plugin> getPlugins() {
        return plugins;
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
        plugins = context.getPluginService().findPlugins();
        Collections.sort(plugins, new Comparator<Plugin>() {
            @Override
            public int compare(Plugin o1, Plugin o2) {
                return Integer.valueOf(o1.getLoadOrder()).compareTo(Integer.valueOf(o2.getLoadOrder()));
            }
        });
        grid.setItems(plugins);
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        boolean enabled = getSelectedItems().size() > 0;
        moveUpButton.setEnabled(enabled);
        moveDownButton.setEnabled(enabled);
    }

    protected Set<Plugin> getSelectedItems() {
        return grid.getSelectedItems();
    }

    protected void moveItemsTo(Set<Plugin> itemIds, int index) {
        if (index >= 0 && index < plugins.size() && itemIds.size() > 0) {
            int firstItemIndex = plugins.indexOf(itemIds.iterator().next());
            if (index != firstItemIndex) {
                for (Plugin itemId : itemIds) {
                    boolean movingUp = index < plugins.indexOf(itemId);
                    plugins.remove(itemId);
                    plugins.add(index, itemId);
                    if (movingUp) {
                        index++;
                    }
                }
            }

            updateLoadOrder();
            refresh();
        }
    }

    protected void updateLoadOrder() {
        int loadOrder = 1;
        for (Plugin plugin : plugins) {
            if (loadOrder != plugin.getLoadOrder()) {
                plugin.setLoadOrder(loadOrder);
                context.getPluginService().save(plugin);
            }
            loadOrder++;
        }

    }

    protected void moveUp() {
        Set<Plugin> itemIds = getSelectedItems();
        if (itemIds.size() > 0 && itemIds != null) {
            Plugin firstItem = itemIds.iterator().next();
            int index = plugins.indexOf(firstItem) - 1;
            moveItemsTo(getSelectedItems(), index);
        }
    }

    protected void moveDown() {
        Set<Plugin> itemIds = getSelectedItems();
        if (itemIds.size() > 0 && itemIds != null) {
            Plugin lastItem = null;
            Iterator<Plugin> iter = itemIds.iterator();
            while (iter.hasNext()) {
                lastItem = iter.next();
            }
            int index = plugins.indexOf(lastItem) + 1;
            moveItemsTo(getSelectedItems(), index);
        }
    }

    protected void addPlugin() {
        new PluginsPanelAddDialog(context, PluginsPanel.this) {
            @Override
            public void close() {
                super.close();
                PluginsPanel.this.refresh();
            }
        }.open();
    }

    protected void purgeUnused() {
        IPluginService pluginService = context.getPluginService();
        List<Plugin> plugins = pluginService.findUnusedPlugins();
        for (Plugin plugin : plugins) {
            pluginService.delete(plugin);
            /*
             * TODO: Before enabling this need to figure out logic to calculate if the
             * plug-in is required by other plug-ins that ARE currently
             * referenced
             */
            // context.getPluginManager().delete(plugin.getArtifactGroup(),
            // plugin.getArtifactName(), plugin.getArtifactVersion());
        }

        if (plugins.size() > 0) {
            refresh();
        }
    }
    

}
