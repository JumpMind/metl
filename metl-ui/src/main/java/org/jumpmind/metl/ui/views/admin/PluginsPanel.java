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

import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.persist.IPluginService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.vaadin.ui.common.IUiPanel;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class PluginsPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    Button addButton;

    Button removeButton;

    Button moveUpButton;

    Button moveDownButton;

    BeanItemContainer<Plugin> container;

    Table table;

    List<Plugin> plugins;

    public PluginsPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        addButton = buttonBar.addButton("Add", FontAwesome.PLUS);
        addButton.addClickListener(e -> addPlugin());

        moveUpButton = buttonBar.addButton("Move Up", FontAwesome.ARROW_UP, e -> moveUp());

        moveDownButton = buttonBar.addButton("Move Down", FontAwesome.ARROW_DOWN, e -> moveDown());

        removeButton = buttonBar.addButton("Purge Unused", FontAwesome.TRASH_O, e -> purgeUnused());

        container = new BeanItemContainer<Plugin>(Plugin.class);

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setSortEnabled(false);

        table.setContainerDataSource(container);
        table.setVisibleColumns("artifactGroup", "artifactName", "artifactVersion", "lastUpdateTime");
        table.setColumnHeaders("Group", "Name", "Version", "Updated");
        table.setColumnWidth("lastUpdateTime", UIConstants.DATETIME_WIDTH_PIXELS);
        table.addValueChangeListener(e -> setButtonsEnabled());

        addComponent(table);
        setExpandRatio(table, 1.0f);

        context.getPluginManager().refresh();

        refresh();
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
        container.removeAllItems();

        plugins = context.getPluginService().findPlugins();
        Collections.sort(plugins, new Comparator<Plugin>() {
            @Override
            public int compare(Plugin o1, Plugin o2) {
                return new Integer(o1.getLoadOrder()).compareTo(new Integer(o2.getLoadOrder()));
            }
        });
        container.addAll(plugins);
        table.sort();
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        boolean enabled = getSelectedItems().size() > 0;
        moveUpButton.setEnabled(enabled);
        moveDownButton.setEnabled(enabled);
    }

    @SuppressWarnings("unchecked")
    protected Set<Plugin> getSelectedItems() {
        return (Set<Plugin>) table.getValue();
    }

    protected void moveItemsTo(Set<Plugin> itemIds, int index) {
        if (index >= 0 && index < container.getItemIds().size() && itemIds.size() > 0) {
            int firstItemIndex = container.indexOfId(itemIds.iterator().next());
            if (index != firstItemIndex) {
                for (Plugin itemId : itemIds) {
                    boolean movingUp = index < container.indexOfId(itemId);
                    container.removeItem(itemId);
                    container.addItemAt(index, itemId);
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
        List<Plugin> plugins = container.getItemIds();
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
            int index = container.indexOfId(firstItem) - 1;
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
            int index = container.indexOfId(lastItem) + 1;
            moveItemsTo(getSelectedItems(), index);
        }
    }

    protected void addPlugin() {
        PluginsPanelAddDialog dialog = new PluginsPanelAddDialog(context, PluginsPanel.this) {
            @Override
            public void close() {
                super.close();
                PluginsPanel.this.refresh();
            }
        };
        UI.getCurrent().addWindow(dialog);
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
