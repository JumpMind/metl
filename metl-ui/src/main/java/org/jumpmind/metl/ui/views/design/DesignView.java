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
package org.jumpmind.metl.ui.views.design;

import jakarta.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.MainLayout;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.metl.ui.common.View;
import org.jumpmind.vaadin.ui.common.TabSheet.EnhancedTab;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

@UiComponent
@UIScope
@PreserveOnRefresh
@TopBarLink(category = Category.Design, name = "Design", id = "design", view = DesignView.class, icon = VaadinIcon.CONNECT, menuOrder = 1, useAsDefault = true)
@Route(value = "design", layout = MainLayout.class)
public class DesignView extends HorizontalLayout implements BeforeEnterObserver, View {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    DesignNavigator projectNavigator;

    TabbedPanel tabbedPanel;

    @PostConstruct
    protected void init() {
        setSizeFull();

        tabbedPanel = new TabbedPanel();
        tabbedPanel.addSelectedTabChangeListener(event -> {
            EnhancedTab tab = (EnhancedTab) event.getSelectedTab();
            if (tab != null) {
                Component component = tab.getComponent();
                if (component instanceof EditFlowPanel) {
                    ((EditFlowPanel) component).redrawFlow();
                }
            }
        });

        SplitLayout leftSplit = new SplitLayout();
        leftSplit.setSizeFull();
        leftSplit.setSplitterPosition(20);

        projectNavigator = new DesignNavigator(context, tabbedPanel);

        leftSplit.addToPrimary(projectNavigator);
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.setPadding(false);
        container.add(tabbedPanel);
        leftSplit.addToSecondary(container);

        add(leftSplit);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        projectNavigator.refresh();
    }

}
