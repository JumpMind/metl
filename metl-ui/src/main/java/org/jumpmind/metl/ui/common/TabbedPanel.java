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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.TabSheet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.tabs.TabsVariant;

public class TabbedPanel extends TabSheet {

    private static final long serialVersionUID = 1L;

    protected EnhancedTab mainTab;

    protected Map<String, EnhancedTab> tabsById = new HashMap<String, EnhancedTab>();

    protected Map<Component, String> contentToId = new HashMap<Component, String>();

    protected List<CloseHandler> closeHandlers = new ArrayList<CloseHandler>();
    
    protected CloseHandler closeHandler;

    protected IUiPanel selectedTab;

    protected List<String> selectedOrder = new ArrayList<>();

    boolean closing = false;

    public TabbedPanel() {
        setSizeFull();
        tabs.addThemeVariants(TabsVariant.LUMO_SMALL);
        
        content.setHeight("96%");

        addSelectedTabChangeListener((event) -> {
            EnhancedTab tab = (EnhancedTab) event.getSelectedTab();
            Component selected = null;
            if (tab != null) {
                selected = tab.getComponent();
            }
            if (selectedTab != null) {
                selectedTab.deselected();
                selectedTab = null;
            }

            if (selected instanceof IUiPanel) {
                selectedTab = ((IUiPanel) selected);
                selectedTab.selected();
            }

            String id = contentToId.get(selectedTab);
            if (id != null && !closing) {
                selectedOrder.add(id);
            }
        });

        setCloseHandler((tabsheet, tabContent) -> close(tabContent));

        ContextMenu menu = new ContextMenu(tabs);
        menu.addItem("Close", selectedItem -> close());
        menu.addItem("Close Others", selectedItem -> closeOthers());
        menu.addItem("Close To the Left", selectedItem -> closeToTheLeft());
        menu.addItem("Close To the Right", selectedItem -> closeToTheRight());
        menu.add(new Hr());
        menu.addItem("Close All", selectedItem -> closeAll());
    }

    protected void close(Component tabContent) {
        String id = contentToId.get(tabContent);
        try {
            closing = true;
            if (tabContent instanceof IUiPanel) {
                if (((IUiPanel) tabContent).closing()) {
                    closeTab(id);
                }
                selectedTab = null;
            } else {
                closeTab(id);
            }

            for (CloseHandler closeHandler : closeHandlers) {
                closeHandler.onTabClose(this, tabContent);
            }

            while (selectedOrder.contains(id)) {
                selectedOrder.remove(id);
            }
        } finally {
            closing = false;
        }

        if (selectedOrder.size() > 0) {
            EnhancedTab selectNext = tabsById.get(selectedOrder.get(selectedOrder.size() - 1));
            if (selectNext != null) {
                setSelectedTab(selectNext);
            }
        }
    }

    public void close() {
        close(getSelectedTab());
    }

    public void closeAll() {
        for(Component next : getChildList()) {
            close(next);
        }
    }

    public void closeToTheLeft() {
        Component selected = getSelectedTab();
        for(Component next : getChildList()) {
            if (!next.equals(selected)) {
                close(next);
            } else {
                break;
            }
        }
    }

    public void closeToTheRight() {
        Component selected = getSelectedTab();
        boolean closing = false;
        for(Component next : getChildList()) {
            if (next.equals(selected)) {
                closing = true;
            } else if (closing) {
                close(next);
            }
        }
    }

    public void closeOthers() {
        Component selected = getSelectedTab();
        for(Component next : getChildList()) {
            if (!next.equals(selected)) {
                close(next);
            }
        }
    }
    
    protected List<Component> getChildList() {
        List<Component> children = new ArrayList<>(getComponentCount());
        Iterator<Component> i = iterator();
        while (i.hasNext()) {
            children.add(i.next());
        }
        return children;
    }

    public void addCloseHandler(CloseHandler handler) {
        this.closeHandlers.add(handler);
    }
    
    protected void setCloseHandler(CloseHandler handler) {
        closeHandler = handler;
    }

    public void setMainTab(String caption, Icon icon, Component component) {
        if (component instanceof HasSize) {
            ((HasSize) component).setSizeFull();
        }
        this.mainTab = add(component, caption, icon, 0);
    }
    
    public void mainTabToTop() {
        this.setSelectedTab(mainTab);
    }

    public boolean closeTab(String id) {
        EnhancedTab tab = tabsById.remove(id);
        if (tab != null) {
            contentToId.remove(tab.getComponent());
            this.remove(tab);
            EnhancedTab selectedTab = (EnhancedTab) tabs.getSelectedTab();
            if (selectedTab != null) {
                Component component = selectedTab.getComponent();
                if (component != null) {
                    component.setVisible(true);
                    content.add(component);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void addCloseableTab(String id, String caption, Icon icon, Component component) {
        EnhancedTab tab = tabsById.get(id);
        if (tab == null) {
            if (component instanceof HasSize) {
                ((HasSize) component).setSizeFull();
            }
            contentToId.put(component, id);
            tab = add(component, caption, icon);
            tab.setCloseable(true);
            tabsById.put(id, tab);
            setSelectedTab(tab);
        } else {
            setSelectedTab(tab);
        }
    }

    @Override
    public void remove(EnhancedTab tab) {
        super.remove(tab);
        if (closeHandler != null) {
            closeHandler.onTabClose(this, tab.getComponent());
        }
    }

    public interface CloseHandler {
        public void onTabClose(TabbedPanel panel, Component component);
    }
}
