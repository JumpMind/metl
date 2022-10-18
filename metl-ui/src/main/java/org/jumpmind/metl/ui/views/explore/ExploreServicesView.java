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

import javax.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.VerticalLayout;

@UiComponent
@Scope("ui")
@TopBarLink(id = "exploreServices", category = Category.Explore, menuOrder = 30, name = "Services", icon = FontAwesome.GLOBE)
public class ExploreServicesView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    public ExploreServicesView() {
        setSizeFull();
        String url = Page.getCurrent()
                .getLocation().getPath();
        BrowserFrame e = new BrowserFrame(null, new ExternalResource(url.substring(0, url.lastIndexOf("/")) + "/ws-api.html"));
        e.setSizeFull();
        addComponent(e);
        
    }
    
    @PostConstruct
    protected void init() {
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }


}
