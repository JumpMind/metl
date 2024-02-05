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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.ui.init.AppUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;

@Component
@Scope(value="ui")
public class ViewManager implements Serializable {

    private static final long serialVersionUID = 1L;

    /*@Autowired(required = false)
    List<View> views;*/
    
    List<Class<?>> views = new ArrayList<Class<?>>();
    
    @Autowired
    List<TopBarButton> topBarButtons;

    public ViewManager() {
        
    }

    public void init(AppUI ui) {
        for (RouteData route : RouteConfiguration.forSessionScope().getAvailableRoutes()) {
            views.add(route.getNavigationTarget());
        }
        List<Class<?>> filteredViews = new ArrayList<Class<?>>();
        if (views != null) {
            for (Class<?> view : views) {
                TopBarLink menu = (TopBarLink) view.getAnnotation(TopBarLink.class);
                if (menu != null && menu.uiClass().equals(AppUI.class)) {
                    filteredViews.add(view);
                }
            }
            views.clear();
            views.addAll(filteredViews);
        }
    }

    public Map<Category, List<TopBarLink>> getMenuItemsByCategory() {
        LinkedHashMap<Category, List<TopBarLink>> menuItemsByCategory = new LinkedHashMap<Category, List<TopBarLink>>();
        Category[] categories = Category.values();
        for (Category category : categories) {
            List<TopBarLink> viewsByCategory = new ArrayList<TopBarLink>();
            for (Class<?> view : views) {
                TopBarLink menu = (TopBarLink) view.getAnnotation(TopBarLink.class);
                if (menu != null) {
                    if (menu.category() == category) {
                        viewsByCategory.add(menu);
                    }
                }
            }
            Collections.sort(viewsByCategory, new Comparator<TopBarLink>() {
                @Override
                public int compare(TopBarLink o1, TopBarLink o2) {
                    return new Integer(o1.menuOrder()).compareTo(new Integer(o2.menuOrder()));
                }
            });
            if (viewsByCategory.size() > 0) {
                menuItemsByCategory.put(category, viewsByCategory);
            }
        }
        return menuItemsByCategory;
    }

    public List<TopBarButton> getTopBarButtons() {
        return topBarButtons;
    }
}
