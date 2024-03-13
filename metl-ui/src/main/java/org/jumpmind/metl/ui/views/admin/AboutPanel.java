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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang.time.FastDateFormat;
import org.jumpmind.metl.core.runtime.AgentManager;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.util.AppUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.UIScope;

@UiComponent
@UIScope
@Order(1400)
@AdminMenuLink(name = "About", id = "About", icon = VaadinIcon.QUESTION)
public class AboutPanel extends AbstractAdminPanel implements IBackgroundRefreshable<Object> {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = 1L;

    Grid<String[]> grid;

    public AboutPanel() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button gcCollect = new Button("Garbage Collect", (e) -> {
            Runtime.getRuntime().gc();
            refresh();
        });
        gcCollect.addThemeVariants(ButtonVariant.LUMO_SMALL);
        add(gcCollect);

        grid = new Grid<String[]>();
        grid.setSizeFull();
        grid.addClassName("noscroll");
        grid.addColumn(item -> item[0]).setHeader("Name").setFlexGrow(0).setWidth("200px");
        grid.addColumn(item -> item[1]).setHeader("Value");
        addAndExpand(grid);
    }
    
    @PostConstruct
    @Override
    public void init() {
        super.init();
        context.getBackgroundRefresherService().register(this);
    }
    

    @Override
    public boolean closing() {
        context.getBackgroundRefresherService().unregister(this);
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
        onBackgroundDataRefresh();
    }

    @Override
    public Object onBackgroundDataRefresh() {
        return new Object();
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        refresh();
    }

    @Override
    public void onUIError(Throwable ex) {
        log.error("", ex);
        CommonUiUtils.notifyError();        
    }

    protected void refresh() {
        List<String[]> itemList = new ArrayList<String[]>();
        itemList.add(new String[] { "Application Version", VersionUtils.getCurrentVersion() });
        itemList.add(new String[] { "Build Time", VersionUtils.getBuildTime() });
        itemList.add(new String[] { "SCM Revision", VersionUtils.getScmVersion() });
        itemList.add(new String[] { "SCM Branch", VersionUtils.getScmBranch() });

        itemList.add(new String[] { "Host Name", AppUtils.getHostName() });
        itemList.add(new String[] { "IP Address", AppUtils.getIpAddress() });
        itemList.add(new String[] { "Java Version", System.getProperty("java.version") });
        itemList.add(new String[] { "System Time",
                FastDateFormat.getTimeInstance(FastDateFormat.MEDIUM).format(new Date()) });
        itemList.add(new String[] { "Used Heap",
                Long.toString(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) });
        itemList.add(new String[] { "Heap Size", Long.toString(Runtime.getRuntime().maxMemory()) });
        itemList.add(new String[] { "Last Restart", CommonUiUtils.formatDateTime(AgentManager.lastRestartTime) });
        grid.setItems(itemList);
    }

}
