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


import java.util.Date;
import org.jumpmind.metl.ui.i18n.MessageSource;
import org.apache.commons.lang.time.FastDateFormat;
import org.jumpmind.metl.core.runtime.AgentManager;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.Table;
import org.jumpmind.util.AppUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class AboutPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable<Object> {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = 1L;

    Table table;

    ApplicationContext context;

    public AboutPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;

        setSizeFull();
        setMargin(true);
        setSpacing(true);

        Button gcCollect = new Button(MessageSource.message("aboutPanel.garbageCollect"), (e) -> {
            Runtime.getRuntime().gc();
            refresh();
        });
        gcCollect.addStyleName(ValoTheme.BUTTON_TINY);
        addComponent(gcCollect);

        table = new Table();
        table.setSizeFull();
        table.addStyleName("noscroll");
        table.addContainerProperty(MessageSource.message("common.name"), String.class, null);
        table.setColumnWidth(MessageSource.message("common.name"), 200);
        table.addContainerProperty(MessageSource.message("common.value"), String.class, null);
        addComponent(table);
        setExpandRatio(table, 1);
        refresh();
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
        CommonUiUtils.notify(ex);        
    }

    protected void refresh() {
        table.removeAllItems();
        int itemId = 0;
     
        table.addItem(new Object[] {MessageSource.message("aboutPanel.applicationVersion") , VersionUtils.getCurrentVersion() },
                itemId++);
        table.addItem(new Object[] {MessageSource.message("aboutPanel.buildTime") , VersionUtils.getBuildTime() }, itemId++);
        table.addItem(new Object[] { MessageSource.message("aboutPanel.SCM.revision"), VersionUtils.getScmVersion() }, itemId++);
        table.addItem(new Object[] { MessageSource.message("aboutPanel.SCM.branch"), VersionUtils.getScmBranch() }, itemId++);

        table.addItem(new Object[] { MessageSource.message("aboutPanel.hostName"), AppUtils.getHostName() }, itemId++);
        table.addItem(new Object[] { MessageSource.message("aboutPanel.IP.address"), AppUtils.getIpAddress() }, itemId++);
        table.addItem(new Object[] { MessageSource.message("aboutPanel.javaVersion"), System.getProperty("java.version") },
                itemId++);
        table.addItem(
                new Object[] { MessageSource.message("aboutPanel.systemTime"),
                        FastDateFormat.getTimeInstance(FastDateFormat.MEDIUM).format(new Date()) },
                itemId++);
        table.addItem(
                new Object[] { MessageSource.message("aboutPanel.usedHeap"), Long.toString(
                        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) },
                itemId++);
        table.addItem(new Object[] { MessageSource.message("aboutPanel.heapSize"), Long.toString(Runtime.getRuntime().maxMemory()) },
                itemId++);
       
        table.addItem(new Object[] {MessageSource.message("aboutPanel.lastRestart"),
                CommonUiUtils.formatDateTime(AgentManager.lastRestartTime) }, itemId++);
    }

}
