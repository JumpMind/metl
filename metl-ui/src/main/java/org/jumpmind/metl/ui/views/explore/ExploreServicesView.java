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
package org.jumpmind.metl.ui.views.explore;

import jakarta.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.MainLayout;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.metl.ui.common.View;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

@UiComponent
@UIScope
@PreserveOnRefresh
@TopBarLink(id = "exploreServices", view = ExploreServicesView.class, category = Category.Explore, menuOrder = 30, name = "Services", icon = VaadinIcon.GLOBE)
@Route(value = "exploreServices", layout = MainLayout.class)
public class ExploreServicesView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    public ExploreServicesView() {
        setSizeFull();
        UI.getCurrent().getPage().fetchCurrentURL(url -> {
            String urlString = url.toString();
            IFrame e = new IFrame(urlString.substring(0, urlString.lastIndexOf("/")) + "/ws-api.html");
            e.setSizeFull();
            add(e);
        });
        
    }
    
    @PostConstruct
    protected void init() {
    }


}
