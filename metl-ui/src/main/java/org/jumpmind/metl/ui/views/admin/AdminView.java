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
import org.springframework.context.annotation.Scope;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ItemClick;
import com.vaadin.ui.Tree.ItemClickListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.Admin, name = "Admin", id = "admin", icon = VaadinIcons.COGS, menuOrder = 10)
public class AdminView extends HorizontalLayout implements View, IUiPanel, ItemClickListener<AdminMenuLink> {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    @Autowired 
    List<AdminSideView> sideMenu;
    
    TabbedPanel tabbedPanel;
    
    Tree<AdminMenuLink> tree;

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

        tree = new Tree<AdminMenuLink>();
        tree.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        tree.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        tree.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        tree.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        tree.setSizeFull();
        //tree.setCacheRate(100);
        //tree.setPageLength(100);
        tree.addItemClickListener(this);
        tree.addStyleName("noselect");
        tree.setItemCaptionGenerator(item -> item.id());
        tree.setItemIconGenerator(item -> item.icon());
        
        for (AdminSideView sideView : sideMenu) {
            AdminMenuLink link = (AdminMenuLink) sideView.getClass().getAnnotation(AdminMenuLink.class);
            sideView.setAdminView(this);
                if (link != null && link.uiClass().equals(AppUI.class) && sideView.isAccessible()) {
                    addItem(link);
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

        navigator.addComponent(tree);
        navigator.setExpandRatio(tree, 1);
        
        addComponent(leftSplit);
        
    }

    protected void addItem(AdminMenuLink link) {
        tree.getTreeData().addItem(null, link);
    }

    public void itemClick(ItemClick<AdminMenuLink> event) {
        if (event.getMouseEventDetails().getButton() == MouseButton.LEFT) {
            AdminMenuLink value = event.getItem();
            if (value != null) {
                String id = value.id();
                Component panel = sideMenuById.get(id);
                tabbedPanel.addCloseableTab(id, id, value.icon(), panel);
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

    protected Tree<AdminMenuLink> getTree() {
        return tree;
    }


}
