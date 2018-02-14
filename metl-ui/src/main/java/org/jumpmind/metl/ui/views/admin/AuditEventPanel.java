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

import java.util.List;

import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.VerticalLayout;

public class AuditEventPanel extends VerticalLayout implements IUiPanel {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = 1L;

    Grid grid;
    
    ApplicationContext context;

    public AuditEventPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        setSizeFull();
        setMargin(true);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
        if (grid != null) {
            removeComponent(grid);
        }
        grid = new Grid();
        BeanContainer<AuditEvent, AuditEvent> container = new BeanContainer<>(AuditEvent.class);
        grid.setContainerDataSource(container);
        grid.setColumnOrder("createTime", "name", "eventText", "lastUpdateBy");
        grid.getColumn("createTime").setHeaderCaption("Event Time");
        grid.getColumn("name").setHeaderCaption("Event Type");
        grid.getColumn("lastUpdateBy").setHeaderCaption("User");
        grid.removeColumn("id");
        grid.removeColumn("createBy");
        grid.removeColumn("lastUpdateTime");
        grid.removeColumn("settingNameAllowed");
        grid.setSizeFull();
        List<AuditEvent> list = context.getOperationsService().findAuditEvents(-1);
        for (AuditEvent event : list) {
            container.addItem(event, event);
        }
        addComponent(grid);
        setExpandRatio(grid, 1);
    }

}
