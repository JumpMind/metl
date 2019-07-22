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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.metl.ui.init.AppUI;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.event.ItemClickEvent;
import com.vaadin.v7.event.ItemClickEvent.ItemClickListener;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Table.ColumnHeaderMode;
import com.vaadin.v7.ui.TreeTable;
import com.vaadin.v7.ui.VerticalLayout;

@UiComponent
@UIScope
@TopBarLink(category = Category.Admin, name = "Admin", id = "admin", icon = FontAwesome.GEARS, menuOrder = 10)
public class AdminView extends HorizontalLayout implements View, IUiPanel, ItemClickListener {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    @Autowired 
    List<AdminSideView> sideMenu;
    
    TabbedPanel tabbedPanel;
    
    TreeTable table;

    Map<String, Component> sideMenuById = new HashMap<String, Component>();
    
    @PostConstruct
    protected void init() {
        setSizeFull();

        tabbedPanel = new TabbedPanel();

        HorizontalSplitPanel leftSplit = new HorizontalSplitPanel();
        leftSplit.setSizeFull();
        leftSplit.setSplitPosition(UIConstants.DEFAULT_LEFT_SPLIT, Unit.PIXELS);

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabbedPanel);
        leftSplit.setSecondComponent(container);

        table = new TreeTable();
        table.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        table.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.addItemClickListener(this);
        table.addStyleName("noselect");
        table.addContainerProperty("id", String.class, null);
        table.setVisibleColumns(new Object[] { "id" });
        table.setColumnExpandRatio("id", 1);
        
        for (AdminSideView sideView : sideMenu) {
            AdminMenuLink link = (AdminMenuLink) sideView.getClass().getAnnotation(AdminMenuLink.class);
            sideView.setAdminView(this);
                if (link != null && link.uiClass().equals(AppUI.class) && sideView.isAccessible()) {
                    addItem(link.id(), link.icon());
                    sideMenuById.put(link.id(), sideView.getView());
                }
        }
        VerticalLayout navigator = new VerticalLayout();
        navigator.addStyleName(ValoTheme.MENU_ROOT);
        navigator.setSizeFull();
        leftSplit.setFirstComponent(navigator);
                
        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);
        navigator.addComponent(leftMenuBar);

        navigator.addComponent(table);
        navigator.setExpandRatio(table, 1);
        
        addComponent(leftSplit);
        
    }

    @SuppressWarnings("unchecked")
    protected void addItem(String id, Resource icon) {
        Item item = table.addItem(id);
        item.getItemProperty("id").setValue(id);
        table.setItemIcon(id, icon);
        table.setChildrenAllowed(id, false);
        table.setCollapsed(id, true);
    }

    public void itemClick(ItemClickEvent event) {
        if (event.getButton() == MouseButton.LEFT) {
            Object value = event.getItemId();
            if (value != null) {
                String id = value.toString();
                Component panel = sideMenuById.get(id);
                tabbedPanel.addCloseableTab(id, id, table.getItemIcon(id), panel);
            }
        }
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }

    @Override
    public boolean closing() {
        return false;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {        
    }

    protected ApplicationContext getContext() {
        return context;
    }

    public TabbedPanel getTabbedPanel() {
        return tabbedPanel;
    }

    protected TreeTable getTable() {
        return table;
    }


}
