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

import com.vaadin.addon.contextmenu.ContextMenu;
import com.vaadin.event.ItemClickEvent.ItemClickNotifier;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.themes.ValoTheme;

abstract public class AbstractMenuBar extends MenuBar {

    private static final long serialVersionUID = 1L;

    Map<Class<?>, ISelectedValueMenuManager> menuActionsByClass;

    protected AbstractSelect parent;

    ISelectedValueMenuManager nothingSelectedMenuManager;

    Handler handler;

    ContextMenu contextMenu;

    public AbstractMenuBar(AbstractSelect parent,
            ISelectedValueMenuManager nothingSelectedMenuManager) {
        addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        setWidth(100, Unit.PERCENTAGE);

        this.parent = parent;
        this.nothingSelectedMenuManager = nothingSelectedMenuManager;
        this.menuActionsByClass = new HashMap<>();
        this.parent.addValueChangeListener((e) -> valueChanged());
        this.handler = new Handler();

        contextMenu = new ContextMenu(parent, false);
        if (parent instanceof ItemClickNotifier) {
            ItemClickNotifier notifier = (ItemClickNotifier)parent;
            notifier.addItemClickListener(e->{
                if (e.getButton()==MouseButton.RIGHT && !e.isDoubleClick()) {
                    contextMenu.open(e.getClientX(), e.getClientY());
                }
            });
        } else {
            parent.addContextClickListener(e->contextMenu.open(e.getClientX(), e.getClientY()));
        }

        buildMenu();

        valueChanged();
    }
    
    public void refresh() {
        valueChanged();
    }
    
    protected void addSeparator(String path) {
        getMenuItem(path).addSeparator();
        getContextMenuItem(path).addSeparator();        
    }

    private String buildMenuString(MenuItem item) {
        StringBuilder menuString = new StringBuilder();
        do {
            menuString.insert(0, item.getText());
            item = item.getParent();
            if (item != null) {
                menuString.insert(0, "|");
            }
        } while (item != null);
        return menuString.toString();
    }

    private String buildMenuString(com.vaadin.addon.contextmenu.MenuItem item) {
        StringBuilder menuString = new StringBuilder();
        do {
            menuString.insert(0, item.getText());
            item = item.getParent();
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
                items = item.getChildren();
            }
            for (MenuItem menuItem : items) {
                if (menuItem.getText().equals(name)) {
                    item = menuItem;
                }
            }
        }
        return item;
    }

    protected com.vaadin.addon.contextmenu.MenuItem getContextMenuItem(String path) {
        com.vaadin.addon.contextmenu.MenuItem item = null;
        String[] names = parse(path);
        for (String name : names) {
            List<com.vaadin.addon.contextmenu.MenuItem> items = null;
            if (item == null) {
                items = contextMenu.getItems();
            } else {
                items = item.getChildren();
            }
            for (com.vaadin.addon.contextmenu.MenuItem menuItem : items) {
                if (menuItem.getText().equals(name)) {
                    item = menuItem;
                }
            }
        }
        return item;
    }

    
    protected void valueChanged() {
        Object selected = parent.getValue();
        ISelectedValueMenuManager action = null;
        if (selected != null) {
            Class<?> clazz = selected.getClass();
            action = menuActionsByClass.get(clazz);
        }

        if (action == null) {
            action = nothingSelectedMenuManager;
        }

        if (action != null) {
            setMenuBarEnabled(action, getItems(), selected);
            setContextMenuEnabled(action, contextMenu.getItems(), selected);
        }
    }

    private void setMenuBarEnabled(ISelectedValueMenuManager action, List<MenuItem> items, Object selected) {
        if (items != null) {
            for (MenuItem menuItem : items) {
                menuItem.setEnabled(action.isEnabled(buildMenuString(menuItem), selected));
                List<MenuItem> children = menuItem.getChildren();
                setMenuBarEnabled(action, children, selected);
            }
        }
    }
    
    private void setContextMenuEnabled(ISelectedValueMenuManager action, List<com.vaadin.addon.contextmenu.MenuItem> items, Object selected) {
        if (items != null) {
            for (com.vaadin.addon.contextmenu.MenuItem menuItem : items) {
                menuItem.setEnabled(action.isEnabled(buildMenuString(menuItem), selected));
                List<com.vaadin.addon.contextmenu.MenuItem> children = menuItem.getChildren();
                setContextMenuEnabled(action, children, selected);
            }
        }
    }    

    abstract protected void buildMenu();

    protected void add(String path) {
        MenuItem item = null;
        com.vaadin.addon.contextmenu.MenuItem contextItem = null;
        for (String name : parse(path)) {
            if (item == null) {
                item = addToMenuBarIfNotExists(name, null, getItems());
            } else {
                item = addToMenuBarIfNotExists(name, item, item.getChildren());
            }

            if (contextItem == null) {
                contextItem = addToContextMenuIfNotExists(name, null, contextMenu.getItems());
            } else {
                contextItem = addToContextMenuIfNotExists(name, contextItem,
                        contextItem.getChildren());
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
                parent.setCommand(null);
            }
            item = parent != null ? parent.addItem(name, handler) : addItem(name, handler);
        }
        return item;
    }

    private com.vaadin.addon.contextmenu.MenuItem addToContextMenuIfNotExists(String name,
            com.vaadin.addon.contextmenu.MenuItem parent,
            List<com.vaadin.addon.contextmenu.MenuItem> items) {
        com.vaadin.addon.contextmenu.MenuItem item = null;
        if (items != null) {
            for (com.vaadin.addon.contextmenu.MenuItem menuItem : items) {
                if (menuItem.getText().equals(name)) {
                    item = menuItem;
                }
            }
        }
        if (item == null) {
            if (parent != null) {
                parent.setCommand(null);
            }
            item = parent != null ? parent.addItem(name, handler)
                    : contextMenu.addItem(name, handler);
        }
        return item;
    }

    class Handler implements Command, com.vaadin.addon.contextmenu.Menu.Command {

        private static final long serialVersionUID = 1L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            menuSelected(buildMenuString(selectedItem));
        }

        @Override
        public void menuSelected(com.vaadin.addon.contextmenu.MenuItem selectedItem) {
            menuSelected(buildMenuString(selectedItem));
        }

        protected void menuSelected(String menuString) {
            Object selected = parent.getValue();
            ISelectedValueMenuManager action = null;
            if (selected != null) {
                Class<?> clazz = selected.getClass();
                action = menuActionsByClass.get(clazz);
            } else {
                action = nothingSelectedMenuManager;
            }

            if (action != null) {
                action.handle(menuString, selected);
            }
        }
    }

}
