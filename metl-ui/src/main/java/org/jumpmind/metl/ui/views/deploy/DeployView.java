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
package org.jumpmind.metl.ui.views.deploy;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.Deploy, name = "Deploy", id = "deploy", icon = FontAwesome.GEARS, menuOrder = 10)
public class DeployView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    static final FontAwesome DEPLOYMENT_ICON = FontAwesome.CUBE;

    @Autowired
    ApplicationContext context;

    DeployNavigator deployNavigator;

    TabbedPanel tabbedPanel;

    @PostConstruct
    protected void init() {
        setSizeFull();

        tabbedPanel = new TabbedPanel();

        HorizontalSplitPanel leftSplit = new HorizontalSplitPanel();
        leftSplit.setSizeFull();
        leftSplit.setSplitPosition(UIConstants.DEFAULT_LEFT_SPLIT, Unit.PIXELS);

        deployNavigator = new DeployNavigator(context, tabbedPanel);

        leftSplit.setFirstComponent(deployNavigator);
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabbedPanel);
        leftSplit.setSecondComponent(container);

        addComponent(leftSplit);

    }

    @Override
    public void enter(ViewChangeEvent event) {
        deployNavigator.refresh();
    }

}
