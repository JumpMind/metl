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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.server.VaadinSession;

public class TopBar extends HorizontalLayout implements AfterNavigationObserver {

    private static final long serialVersionUID = 1L;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    MenuBar menuBar;

    ViewManager viewManager;
    
    String defaultView;

    Map<String, MenuItem> viewToButtonMapping;
    
    Map<String, MenuItem> childToParentMapping;

    ApplicationContext context;

    List<MenuItem> categoryItems = new ArrayList<MenuItem>();

    public TopBar(ViewManager vm, ApplicationContext context) {
        setWidthFull();
        this.context = context;
        this.viewManager = vm;

        viewToButtonMapping = new HashMap<String, MenuItem>();
        childToParentMapping = new HashMap<String, MenuItem>();

        menuBar = new MenuBar();
        menuBar.setWidthFull();
        addAndExpand(menuBar);

        for (TopBarButton topBarButton : viewManager.getTopBarButtons()) {
            topBarButton.getStyle().set("margin-top", "0").set("margin-bottom", "0");
            add(topBarButton);
        }

        Button logoutButton = new Button("Logout", new Icon(VaadinIcon.SIGN_OUT));
        logoutButton.getStyle().set("margin-top", "0").set("margin-bottom", "0");
        logoutButton.addClickListener(event -> logout());
        add(logoutButton);

        Map<Category, List<TopBarLink>> menuItemsByCategory = viewManager.getMenuItemsByCategory();
        Set<Category> categories = menuItemsByCategory.keySet();
        for (Category category : categories) {
            if (!context.getUser().hasPrivilege(category.name())) {
                log.info("'{}' does not have access to the {} menu tab", context.getUser(), category.name());
                continue;
            }
            List<TopBarLink> links = menuItemsByCategory.get(category);
            boolean needDefaultView = defaultView == null && links.size() > 0;
            MenuItem categoryItem = null;
            if (links.size() > 1) {
                categoryItem = menuBar.addItem(category.name());
                categoryItems.add(categoryItem);
            }

            if (needDefaultView) {
                defaultView = links.get(0).id();
            }

            for (final TopBarLink menuLink : links) {
                ComponentEventListener<ClickEvent<MenuItem>> command = new ComponentEventListener<ClickEvent<MenuItem>>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onComponentEvent(ClickEvent<MenuItem> event) {
                        setMenuItemFocus(event.getSource());
                        UI.getCurrent().navigate(menuLink.view());
                    }
                };
                MenuItem menuItem = null;
                if (categoryItem == null) {
                    menuItem = menuBar.addItem(menuLink.name(), command);
                } else {
                    menuItem = categoryItem.getSubMenu().addItem(menuLink.name(), command);
                    childToParentMapping.put(menuLink.name(), categoryItem);
                }
                viewToButtonMapping.put(menuLink.id(), menuItem);
            }
        }
        UI.getCurrent().getPage().fetchCurrentURL(url -> {
            String path = url.getPath();
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash > -1) {
                MenuItem menuItem = viewToButtonMapping.get(path.substring(lastSlash + 1));
                setMenuItemFocus(menuItem);
            } else {
                setMenuItemFocus(null);
            }
        });
    }

    protected void logout() {
        Page page = UI.getCurrent().getPage();
        page.fetchCurrentURL(url -> {
            VaadinSession.getCurrent().close();
            page.setLocation(url.toString());
        });
    }
    
    protected void setMenuItemFocus(MenuItem focusItem) {
        for (MenuItem item : categoryItems) {
            item.getElement().getStyle().set("color", "var(--lumo-header-text-color)");
        }
        for (MenuItem item : viewToButtonMapping.values()) {
            item.getElement().getStyle().set("color", "var(--lumo-header-text-color)");
        }
        if (focusItem != null) {
            focusItem.getElement().getStyle().set("color", "var(--lumo-primary-color)");
            MenuItem parentItem = childToParentMapping.get(focusItem.getText());
            if (parentItem != null) {
                parentItem.getElement().getStyle().set("color", "var(--lumo-primary-color)");
            }
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String view = event.getLocation().getFirstSegment();
        if (isBlank(view)) {
            view = defaultView;
        }
        MenuItem menuItem = viewToButtonMapping.get(view);
        if (menuItem != null) {
            setMenuItemFocus(menuItem);
        }
    }

}
