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
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.ValoTheme;

public class TabbedPanel extends TabSheet {

    private static final long serialVersionUID = 1L;

    protected Tab mainTab;

    protected Map<String, Tab> tabsById = new HashMap<String, Tab>();

    protected Map<Component, String> contentToId = new HashMap<Component, String>();
    
    protected List<CloseHandler> closeHandlers = new ArrayList<TabSheet.CloseHandler>();
    
    protected IUiPanel selectedTab;

    public TabbedPanel() {
        setSizeFull();
        addStyleName(ValoTheme.TABSHEET_FRAMED);

        addSelectedTabChangeListener(new SelectedTabChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void selectedTabChange(SelectedTabChangeEvent event) {
                Component selected = event.getTabSheet().getSelectedTab();
                if (selectedTab != null) {
                    selectedTab.deselected();
                    selectedTab = null;
                }
                
                if (selected instanceof IUiPanel) {
                    selectedTab = ((IUiPanel) selected); 
                    selectedTab.selected();
                    
                }
            }
        });

        setCloseHandler(new CloseHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onTabClose(TabSheet tabsheet, Component tabContent) {
                if (tabContent instanceof IUiPanel) {
                    if (((IUiPanel) tabContent).closing()) {
                        closeTab(contentToId.get(tabContent));
                    }
                    selectedTab = null;
                } else {
                    closeTab(contentToId.get(tabContent));
                }
                
                for (CloseHandler closeHandler : closeHandlers) {
                    closeHandler.onTabClose(tabsheet, tabContent);                    
                }
            }
        });
    }
    
    public void addCloseHandler(CloseHandler handler) {
        this.closeHandlers.add(handler);
    }

    public void setMainTab(String caption, Resource icon, Component component) {
        component.setSizeFull();
        this.mainTab = addTab(component, caption, icon, 0);
    }

    public boolean closeTab(String id) {
        Tab tab = tabsById.remove(id);
        if (tab != null) {
            contentToId.remove(tab.getComponent());
            this.removeTab(tab);
            return true;
        } else {
            return false;
        }
    }

    public void addCloseableTab(String id, String caption, Resource icon, Component component) {
        Tab tab = tabsById.get(id);
        if (tab == null) {
            component.setSizeFull();
            tab = addTab(component, caption, icon);
            tab.setClosable(true);
            setSelectedTab(tab);
            tabsById.put(id, tab);
            contentToId.put(component, id);
        } else {
            setSelectedTab(tab);
        }
    }

}
