package org.jumpmind.metl.ui.common;

import static org.jumpmind.metl.core.util.AppConstants.DEFAULT_GROUP;
import static org.jumpmind.metl.core.util.AppConstants.DEFAULT_USER;
import static org.jumpmind.metl.ui.common.UiUtils.whereAreYou;

import java.util.Date;

import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.GroupPrivilege;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.model.UserGroup;
import org.jumpmind.metl.core.model.AuditEvent.EventType;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.metl.ui.init.AppSession;
import org.jumpmind.metl.ui.init.BackgroundRefresherService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

import jakarta.servlet.http.HttpServletRequest;

@PreserveOnRefresh
public class MainLayout extends VerticalLayout implements RouterLayout {
    private static final long serialVersionUID = 1L;
    BackgroundRefresherService backgroundRefresherService;
    AppSession appSession;
    VerticalLayout contentArea = new VerticalLayout();

    public MainLayout() {
        getElement().getClassList().add("apptheme");
        
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
        initMenu();
    }
    
    protected void login(User user) {        
        appSession.setUser(user);
        AppSession.addAppSession(appSession);
    }
    
    protected void initMenu() {
        WebApplicationContext ctx = getWebApplicationContext();
        User user = appSession.getUser();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        contentArea.setWidthFull();
        contentArea.setHeight("0");
        contentArea.setPadding(false);
        contentArea.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)").set("border-bottom",
                "1px solid var(--lumo-contrast-10pct)");

        ApplicationContext appCtx = ctx.getBean(ApplicationContext.class);
        appCtx.setUser(user);
        
        appCtx.getConfigurationService().save(new AuditEvent(EventType.LOGIN, "Logged in", user.getLoginId()));
        user.setLastLoginTime(new Date());
        user.setFailedLogins(0);
        appCtx.getOperationsService().save(user);
        getViewManager().init();

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

        add(menu, contentArea, bottom);
        expand(contentArea);
    }

    protected ViewManager getViewManager() {
        return getWebApplicationContext().getBean(ViewManager.class);
    }
    
    @Override
    public void showRouterLayoutContent(HasElement content) {
        contentArea.getElement().appendChild(content.getElement());
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
}
