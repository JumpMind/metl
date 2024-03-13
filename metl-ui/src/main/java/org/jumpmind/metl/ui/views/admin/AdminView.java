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

import jakarta.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.MainLayout;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.metl.ui.common.View;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

@UiComponent
@UIScope
@PreserveOnRefresh
@TopBarLink(category = Category.Admin, name = "Admin", id = "admin", view = AdminView.class, icon = VaadinIcon.COGS, menuOrder = 10)
@Route(value = "admin", layout = MainLayout.class)
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
        container.setPadding(false);
        container.add(tabbedPanel);
        leftSplit.addToSecondary(container);

        tree = new TreeGrid<AdminMenuLink>();
        tree.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        tree.setSizeFull();
        tree.setPageSize(100);
        tree.addItemClickListener(this);
        tree.addClassName("noselect");
        tree.addComponentHierarchyColumn(item -> {
            Icon icon = new Icon(item.icon());
            icon.getStyle().set("min-width", "24px");
            return new HorizontalLayout(icon, new Span(item.id()));
        });
        
        for (AdminSideView sideView : sideMenu) {
            AdminMenuLink link = (AdminMenuLink) sideView.getClass().getAnnotation(AdminMenuLink.class);
            sideView.setAdminView(this);
                if (link != null && sideView.isAccessible()) {
                    addItem(link);
                    sideMenuById.put(link.id(), sideView.getView());
                }
        }
        leftSplit.addToPrimary(tree);
        
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
                tabbedPanel.setSelectedTab(panel);
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
