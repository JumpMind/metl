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
package org.jumpmind.metl.ui.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AbstractNamedObject;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu.GridContextMenuItemClickEvent;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.shared.Registration;

abstract public class AbstractMenuBar extends MenuBar {

    private static final long serialVersionUID = 1L;

    Map<Class<?>, ISelectedValueMenuManager> menuActionsByClass;
    
    Map<MenuItemBase<?, ?, ?>, Registration> registrationMap;
    
    Map<MenuItemBase<?, ?, ?>, MenuItemBase<?, ?, ?>> parentMap;

    protected TreeGrid<AbstractNamedObject> parent;

    ISelectedValueMenuManager nothingSelectedMenuManager;

    MenuBarListener menuBarListener;
    
    ContextMenuListener contextMenuListener;

    GridContextMenu<AbstractNamedObject> contextMenu;
    
    AbstractNamedObject lastClickedItem;

    public AbstractMenuBar(TreeGrid<AbstractNamedObject> parent,
            ISelectedValueMenuManager nothingSelectedMenuManager) {
        setWidthFull();

        this.parent = parent;
        this.nothingSelectedMenuManager = nothingSelectedMenuManager;
        this.menuActionsByClass = new HashMap<>();
        this.registrationMap = new HashMap<MenuItemBase<?, ?, ?>, Registration>();
        this.parentMap = new HashMap<MenuItemBase<?, ?, ?>, MenuItemBase<?, ?, ?>>();
        this.parent.addSelectionListener((e) -> valueChanged(null));
        this.menuBarListener = new MenuBarListener();
        this.contextMenuListener = new ContextMenuListener();

        contextMenu = parent.addContextMenu();
        contextMenu.setDynamicContentHandler(item -> {
            lastClickedItem = item;
            valueChanged(lastClickedItem);
            return true;
        });

        buildMenu();

        valueChanged(null);
    }
    
    public void refresh() {
        valueChanged(null);
    }
    
    protected void addSeparator(String path) {
        getMenuItem(path).getSubMenu().add(new Hr());
        getContextMenuItem(path).getSubMenu().add(new Hr());    
    }

    private String buildMenuString(MenuItemBase<?, ?, ?> item) {
        StringBuilder menuString = new StringBuilder();
        do {
            menuString.insert(0, item.getText());
            item = parentMap.get(item);
            if (item != null) {
                menuString.insert(0, "|");
            }
        } while (item != null);
        return menuString.toString();
    }

    private String[] parse(String path) {
        return path.split("\\|");
    }

    protected MenuItem getMenuItem(String path) {
        MenuItem item = null;
        String[] names = parse(path);
        for (String name : names) {
            List<MenuItem> items = null;
            if (item == null) {
                items = getItems();
            } else {
                items = item.getSubMenu().getItems();
            }
            for (MenuItem menuItem : items) {
                if (menuItem.getText().equals(name)) {
                    item = menuItem;
                }
            }
        }
        return item;
    }

    protected MenuItemBase<?, ?, ?> getContextMenuItem(String path) {
        MenuItemBase<?, ?, ?> item = null;
        String[] names = parse(path);
        for (String name : names) {
            List<?> items = null;
            if (item == null) {
                items = contextMenu.getItems();
            } else {
                items = item.getSubMenu().getItems();
            }
            for (Object menuItem : items) {
                if (((HasText) menuItem).getText().equals(name)) {
                    item = (MenuItemBase<?, ?, ?>) menuItem;
                }
            }
        }
        return item;
    }

    
    protected void valueChanged(AbstractNamedObject clickedItem) {
        AbstractNamedObject selectedItem = parent.getSelectionModel().getFirstSelectedItem().orElse(null);
        ISelectedValueMenuManager menuBarAction = null;
        ISelectedValueMenuManager contextMenuAction = null;
        if (selectedItem != null) {
            Class<?> clazz = selectedItem.getClass();
            menuBarAction = menuActionsByClass.get(clazz);
        }
        if (clickedItem != null) {
            Class<?> clazz = clickedItem.getClass();
            contextMenuAction = menuActionsByClass.get(clazz);
        }

        if (menuBarAction == null) {
            menuBarAction = nothingSelectedMenuManager;
        }
        if (contextMenuAction == null) {
            contextMenuAction = nothingSelectedMenuManager;
        }

        if (menuBarAction != null) {
            setMenuBarEnabled(menuBarAction, getItems(), selectedItem);
            setContextMenuEnabled(contextMenuAction, contextMenu.getItems(), clickedItem);
        }
    }

    private void setMenuBarEnabled(ISelectedValueMenuManager action, List<MenuItem> items, AbstractNamedObject selected) {
        if (items != null) {
            for (MenuItem menuItem : items) {
                menuItem.setEnabled(action.isEnabled(buildMenuString(menuItem), selected));
                List<MenuItem> children = menuItem.getSubMenu().getItems();
                setMenuBarEnabled(action, children, selected);
            }
        }
    }
    
    private void setContextMenuEnabled(ISelectedValueMenuManager action, List<GridMenuItem<AbstractNamedObject>> items,
            AbstractNamedObject selected) {
        if (items != null) {
            for (GridMenuItem<AbstractNamedObject> menuItem : items) {
                menuItem.setEnabled(action.isEnabled(buildMenuString(menuItem), selected));
                List<GridMenuItem<AbstractNamedObject>> children = menuItem.getSubMenu().getItems();
                setContextMenuEnabled(action, children, selected);
            }
        }
    }    

    abstract protected void buildMenu();

    protected void add(String path) {
        MenuItem item = null;
        GridMenuItem<AbstractNamedObject> contextItem = null;
        for (String name : parse(path)) {
            if (item == null) {
                item = addToMenuBarIfNotExists(name, null, getItems());
            } else {
                item = addToMenuBarIfNotExists(name, item, item.getSubMenu().getItems());
            }

            if (contextItem == null) {
                contextItem = addToContextMenuIfNotExists(name, null, contextMenu.getItems());
            } else {
                contextItem = addToContextMenuIfNotExists(name, contextItem,
                        contextItem.getSubMenu().getItems());
            }
        }

    }

    protected void addMenuManager(Class<?> clazz, ISelectedValueMenuManager action) {
        if (clazz != null && action != null) {
            if (!menuActionsByClass.containsKey(clazz)
                    || menuActionsByClass.get(clazz).equals(action)) {
                menuActionsByClass.put(clazz, action);
            } else {
                throw new IllegalStateException(
                        "Cannot add multiple actions for the same class: " + clazz.getName());
            }
        }
    }

    private MenuItem addToMenuBarIfNotExists(String name, MenuItem parent, List<MenuItem> items) {
        MenuItem item = null;
        if (items != null) {
            for (MenuItem menuItem : items) {
                if (menuItem.getText().equals(name)) {
                    item = menuItem;
                }
            }
        }
        if (item == null) {
            if (parent != null) {
                Registration parentRegistration = registrationMap.get(parent);
                if (parentRegistration != null) {
                    parentRegistration.remove();
                    registrationMap.remove(parent);
                }
            }
            item = parent != null ? parent.getSubMenu().addItem(name) : addItem(name);
            registrationMap.put(item, item.addClickListener(menuBarListener));
            parentMap.put(item, parent);
        }
        return item;
    }

    private GridMenuItem<AbstractNamedObject> addToContextMenuIfNotExists(String name, GridMenuItem<AbstractNamedObject> parent,
            List<GridMenuItem<AbstractNamedObject>> items) {
        GridMenuItem<AbstractNamedObject> item = null;
        if (items != null) {
            for (GridMenuItem<AbstractNamedObject> menuItem : items) {
                if (menuItem.getText().equals(name)) {
                    item = menuItem;
                }
            }
        }
        if (item == null) {
            if (parent != null) {
                Registration parentRegistration = registrationMap.get(parent);
                if (parentRegistration != null) {
                    parentRegistration.remove();
                    registrationMap.remove(parent);
                }
            }
            item = parent != null ? parent.getSubMenu().addItem(name) : contextMenu.addItem(name);
            registrationMap.put(item, item.addMenuItemClickListener(contextMenuListener));
            parentMap.put(item, parent);
        }
        return item;
    }

    class MenuBarListener implements ComponentEventListener<ClickEvent<MenuItem>> {

        private static final long serialVersionUID = 1L;

        @Override
        public void onComponentEvent(ClickEvent<MenuItem> event) {
            menuSelected(buildMenuString(event.getSource()), true);
        }
    }
    
    class ContextMenuListener implements ComponentEventListener<GridContextMenuItemClickEvent<AbstractNamedObject>> {

        private static final long serialVersionUID = 1L;

        @Override
        public void onComponentEvent(GridContextMenuItemClickEvent<AbstractNamedObject> event) {
            menuSelected(buildMenuString(event.getSource()), false);
        }
    }
    
    protected void menuSelected(String menuString, boolean useSelectedItem) {
        AbstractNamedObject item;
        if (useSelectedItem) {
            item = parent.getSelectionModel().getFirstSelectedItem().orElse(null);
        } else {
            item = lastClickedItem;
        }
        ISelectedValueMenuManager action = null;
        if (item != null) {
            Class<?> clazz = item.getClass();
            action = menuActionsByClass.get(clazz);
        } else {
            action = nothingSelectedMenuManager;
        }

        if (action != null) {
            action.handle(menuString, item);
        }
    }

}
