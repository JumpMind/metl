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
package org.jumpmind.metl.ui.init;

import static org.jumpmind.metl.core.util.AppConstants.DEFAULT_GROUP;
import static org.jumpmind.metl.core.util.AppConstants.DEFAULT_USER;
import static org.jumpmind.metl.ui.common.UiUtils.whereAreYou;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.AuditEvent.EventType;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.GroupPrivilege;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.model.UserGroup;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.TopBar;
import org.jumpmind.metl.ui.common.ViewManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Metl")
@PreserveOnRefresh
@Push()
public class AppUI extends UI {

    private static final long serialVersionUID = 1L;

    ViewManager viewManager;

    BackgroundRefresherService backgroundRefresherService;

    AppSession appSession;

    public AppUI() {
        getElement().getClassList().add("apptheme");
    }

    @Override
    protected void init(VaadinRequest request) {
        HttpServletRequest req = ((VaadinServletRequest) VaadinService.getCurrentRequest())
                .getHttpServletRequest();
        appSession = new AppSession(req.getRemoteUser(), whereAreYou(req), req.getRemoteHost(),
                VaadinSession.getCurrent(), req.getHeader("User-Agent"), new Date());

        WebApplicationContext ctx = getWebApplicationContext();

        backgroundRefresherService = ctx.getBean(BackgroundRefresherService.class);
        backgroundRefresherService.init(this);

        afterInit();
    }

    public static WebApplicationContext getWebApplicationContext() {
        VaadinServlet servlet = VaadinServlet.getCurrent();
        if (servlet != null) {
        return WebApplicationContextUtils
                .getRequiredWebApplicationContext(servlet.getServletContext());
        } else {
            return null;
        }
    }

    @Override
    public void onDetach(DetachEvent detachEvent) {
        if (backgroundRefresherService != null) {
            backgroundRefresherService.destroy();
        }
        if (appSession != null) {
            AppSession.remove(appSession);
        }
        super.onDetach(detachEvent);

    }

    protected void afterInit() {
        WebApplicationContext ctx = getWebApplicationContext();
        ApplicationContext appCtx = ctx.getBean(ApplicationContext.class);
        IOperationsService operationsService = appCtx.getOperationsService();
        User user = operationsService.findUserByLoginId(DEFAULT_USER);
        if (user == null) {
            user = new User();
            user.setLoginId(DEFAULT_USER);
            operationsService.save(user);

            Group group = operationsService.findGroupByName(DEFAULT_GROUP);
            if (group == null) {
                group = new Group(DEFAULT_GROUP);
                user.getGroups().add(group);
                operationsService.save(group);
                for (Privilege priv : Privilege.values()) {
                    GroupPrivilege groupPriv = new GroupPrivilege(group.getId(), priv.name());
                    group.getGroupPrivileges().add(groupPriv);
                    operationsService.save(groupPriv);
                }
            }

            UserGroup userGroup = new UserGroup(user.getId(), group.getId());
            operationsService.save(userGroup);
        }
        appCtx.setUser(user);
        login(user);
        access(() -> initMenu());
    }

    protected void login(User user) {        
        appSession.setUser(user);
        AppSession.addAppSession(appSession);
    }
    
    protected void initMenu() {
        WebApplicationContext ctx = getWebApplicationContext();
        User user = appSession.getUser();

        VerticalLayout root = new VerticalLayout();
        root.setSizeFull();
        add(root);

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();

        ApplicationContext appCtx = ctx.getBean(ApplicationContext.class);
        appCtx.setUser(user);
        
        appCtx.getConfigurationService().save(new AuditEvent(EventType.LOGIN, "Logged in", user.getLoginId()));
        user.setLastLoginTime(new Date());
        user.setFailedLogins(0);
        appCtx.getOperationsService().save(user);
        getViewManager().init(this);

        TopBar menu = new TopBar(getViewManager(), appCtx);

        HorizontalLayout bottom = new HorizontalLayout();
        bottom.setWidthFull();
        bottom.getStyle().set("margin", "0 16px");

        HorizontalLayout left = new HorizontalLayout();
        left.setSpacing(true);

        Html logo = new Html("<a href='http://www.jumpmind.com'><img src='VAADIN/themes/apptheme/images/powered-by-jumpmind.png'/></a>");
        bottom.add(logo);

        Span version = new Span("version " + VersionUtils.getCurrentVersion());
        left.add(version);

        bottom.addAndExpand(new Span());
        bottom.add(left);
        bottom.setVerticalComponentAlignment(Alignment.CENTER, left);

        root.add(menu, contentArea, bottom);
        root.expand(contentArea);
    }

    protected ViewManager getViewManager() {
        return getWebApplicationContext().getBean(ViewManager.class);
    }
}
