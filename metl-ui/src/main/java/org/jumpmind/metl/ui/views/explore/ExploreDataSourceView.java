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

import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.metl.ui.common.View;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.DbProvider;
import org.jumpmind.metl.ui.common.MainLayout;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.jumpmind.vaadin.ui.sqlexplorer.SqlExplorer;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

@UiComponent
@UIScope
@PreserveOnRefresh
@TopBarLink(id = "exploreDataSources", view = ExploreDataSourceView.class, category = Category.Explore, menuOrder = 10, name = "DataSource", icon = VaadinIcon.DATABASE)
@Route(value = "exploreDataSources", layout = MainLayout.class)
public class ExploreDataSourceView extends VerticalLayout implements BeforeEnterObserver, View {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;
    
    DbProvider dbProvider;
    
    SqlExplorer explorer;

    public ExploreDataSourceView() {
        setSizeFull();
    }
    
    @PostConstruct
    protected void init () {
        setPadding(false);
        dbProvider = new DbProvider(context);
        explorer = new SqlExplorer(context.getConfigDir(),
                dbProvider, context.getUser().getLoginId(), UIConstants.DEFAULT_LEFT_SPLIT);
        explorer.setSplitterPosition(20);
        add(explorer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        dbProvider.refresh();
        explorer.refresh();
    }


}
