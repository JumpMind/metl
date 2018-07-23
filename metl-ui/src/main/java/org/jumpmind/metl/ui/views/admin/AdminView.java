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

import javax.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.metl.ui.i18n.MenuResource;
import org.jumpmind.metl.ui.i18n.MenuResource;
import org.jumpmind.metl.ui.i18n.MessageSource;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Item;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.Admin, name = "Admin", id = "admin", icon = FontAwesome.GEARS, menuOrder = 10)
public class AdminView extends HorizontalLayout implements View, IUiPanel, ItemClickListener {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    TabbedPanel tabbedPanel;
    
    TreeTable table;

    @PostConstruct
    protected void init() {
        setSizeFull();

        tabbedPanel = new TabbedPanel();

        HorizontalSplitPanel leftSplit = new HorizontalSplitPanel();
        leftSplit.setSizeFull();
        leftSplit.setSplitPosition(UIConstants.DEFAULT_LEFT_SPLIT, Unit.PIXELS);

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabbedPanel);
        leftSplit.setSecondComponent(container);

        table = new TreeTable();
        table.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        table.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.addItemClickListener(this);
        table.addStyleName("noselect");
        table.addContainerProperty("id", String.class, null);
        table.setVisibleColumns(new Object[] { "id" });
        table.setColumnExpandRatio("id", 1);
        addItem(MessageSource.message("adminView.users"), Icons.USER);
        addItem(MessageSource.message("adminView.groups"), Icons.GROUP);
        addItem(MessageSource.message("common.tags"), Icons.TAG);
        addItem(MessageSource.message("adminView.rest"), Icons.REST);
        addItem(MessageSource.message("adminView.generalSettings"), Icons.SETTINGS);
        addItem(MessageSource.message("adminView.pluginRepositories"), Icons.DATABASE);
        addItem(MessageSource.message("adminView.plugins"), Icons.COMPONENT);
        addItem(MessageSource.message("adminView.mailServer"), Icons.EMAIL);
        addItem(MessageSource.message("adminView.ldap"),Icons.BOOK);
        addItem(MessageSource.message("adminView.notifications"), Icons.NOTIFICATION);
        addItem(MessageSource.message("adminView.activeUsers"), FontAwesome.USERS);
        addItem(MessageSource.message("adminView.auditEvents"), FontAwesome.BARS);
        addItem(MessageSource.message("adminView.logging"), Icons.LOGGING);
        addItem(MessageSource.message("adminView.about"), FontAwesome.QUESTION);
        
        VerticalLayout navigator = new VerticalLayout();
        navigator.addStyleName(ValoTheme.MENU_ROOT);
        navigator.setSizeFull();
        leftSplit.setFirstComponent(navigator);
                
        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);
        navigator.addComponent(leftMenuBar);

        navigator.addComponent(table);
        navigator.setExpandRatio(table, 1);
        
        addComponent(leftSplit);
        
    }

    @SuppressWarnings("unchecked")
    protected void addItem(String id, Resource icon) {
        Item item = table.addItem(id);
        item.getItemProperty("id").setValue(id);
        table.setItemIcon(id, icon);
        table.setChildrenAllowed(id, false);
        table.setCollapsed(id, true);
    }

    public void itemClick(ItemClickEvent event) {
        if (event.getButton() == MouseButton.LEFT) {
            Object value = event.getItemId();
            if (value != null) {
                String id = value.toString();
                Component panel = null;
                if (id.equals(MessageSource.message("adminView.users"))) {
                    panel = new UserPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.groups"))) {
                    panel = new GroupPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("common.tags"))) {
                    panel = new TagPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.rest"))) {
                    panel = new ApiPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.generalSettings"))) {
                    panel = new GeneralSettingsPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.mailServer"))) {
                    panel = new MailServerPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.ldap"))) {
                    panel = new LdapPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.notifications"))) {
                    panel = new NotificationPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.logging"))) {
                    panel = new LoggingPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.pluginRepositories") )) {
                    panel = new PluginRepositoriesPanel(context, tabbedPanel);                    
                } else if (id.equals(MessageSource.message("adminView.about"))) {
                    panel = new AboutPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.plugins"))) {
                    panel = new PluginsPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.activeUsers"))) {
                    panel = new ActiveUsersPanel(context, tabbedPanel);
                } else if (id.equals(MessageSource.message("adminView.auditEvents"))) {
                    panel = new AuditEventPanel(context, tabbedPanel);
                }
                tabbedPanel.addCloseableTab(id, id, table.getItemIcon(id), panel);
            }
        }
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }

    @Override
    public boolean closing() {
        return false;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {        
    }
}
