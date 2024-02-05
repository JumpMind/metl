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
import org.jumpmind.metl.ui.common.View;
import org.jumpmind.metl.ui.init.AppUI;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.Admin, name = "Admin", id = "admin", icon = VaadinIcon.COGS, menuOrder = 10)
@Route("admin")
public class AdminView extends HorizontalLayout implements IUiPanel, ComponentEventListener<ItemClickEvent<AdminMenuLink>>, View {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    @Autowired 
    List<AdminSideView> sideMenu;
    
    TabbedPanel tabbedPanel;
    
    TreeGrid<AdminMenuLink> tree;

    Map<String, Component> sideMenuById = new HashMap<String, Component>();
    
    @PostConstruct
    protected void init() {
        setSizeFull();

        tabbedPanel = new TabbedPanel();

        SplitLayout leftSplit = new SplitLayout();
        leftSplit.setSizeFull();
        leftSplit.setSplitterPosition(20);

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.add(tabbedPanel);
        leftSplit.addToSecondary(container);

        tree = new TreeGrid<AdminMenuLink>();
        tree.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        tree.setSizeFull();
        tree.setPageSize(100);
        tree.addItemClickListener(this);
        tree.addClassName("noselect");
        tree.addComponentColumn(item -> new HorizontalLayout(new Icon(item.icon()), new Span(item.id())));
        
        for (AdminSideView sideView : sideMenu) {
            AdminMenuLink link = (AdminMenuLink) sideView.getClass().getAnnotation(AdminMenuLink.class);
            sideView.setAdminView(this);
                if (link != null && link.uiClass().equals(AppUI.class) && sideView.isAccessible()) {
                    addItem(link);
                    sideMenuById.put(link.id(), sideView.getView());
                }
        }
        VerticalLayout navigator = new VerticalLayout();
        navigator.setSizeFull();
        leftSplit.addToPrimary(navigator);
                
        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.setWidthFull();
        navigator.add(leftMenuBar);

        navigator.addAndExpand(tree);
        
        add(leftSplit);
        
    }

    protected void addItem(AdminMenuLink link) {
        tree.getTreeData().addItem(null, link);
    }

    public void onComponentEvent(ItemClickEvent<AdminMenuLink> event) {
        if (event.getButton() == 0) {
            AdminMenuLink value = event.getItem();
            if (value != null) {
                String id = value.id();
                Component panel = sideMenuById.get(id);
                tabbedPanel.addCloseableTab(id, id, new Icon(value.icon()), panel);
            }
        }
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

    protected TreeGrid<AdminMenuLink> getTree() {
        return tree;
    }


}
