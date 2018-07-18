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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
import org.jumpmind.metl.ui.init.LoginDialog.LoginListener;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.DefaultConverterFactory;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.data.util.converter.StringToBooleanConverter;
import com.vaadin.data.util.converter.StringToDateConverter;
import com.vaadin.data.util.converter.StringToDoubleConverter;
import com.vaadin.data.util.converter.StringToFloatConverter;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.util.converter.StringToLongConverter;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Theme("apptheme")
@Title("Metl")
@PreserveOnRefresh
@Push(value = PushMode.AUTOMATIC)
public class AppUI extends UI implements LoginListener {

    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(getClass());

    ViewManager viewManager;

    BackgroundRefresherService backgroundRefresherService;

    AppSession appSession;

    @SuppressWarnings("serial")
    @Override
    protected void init(VaadinRequest request) {
        HttpServletRequest req = ((VaadinServletRequest) VaadinService.getCurrentRequest())
                .getHttpServletRequest();
        appSession = new AppSession(req.getRemoteUser(), whereAreYou(req), req.getRemoteHost(),
                VaadinSession.getCurrent(), req.getHeader("User-Agent"), new Date());

        WebApplicationContext ctx = getWebApplicationContext();

        backgroundRefresherService = ctx.getBean(BackgroundRefresherService.class);
        backgroundRefresherService.init(this);

        setErrorHandler(new DefaultErrorHandler() {
            public void error(com.vaadin.server.ErrorEvent event) {
                String intro = "Exception of type <b>";
                String message = "";
                for (Throwable t = event.getThrowable(); t != null; t = t.getCause()) {
                    if (t.getCause() == null) {
                        intro += t.getClass().getName()
                                + "</b> with the following message:<br/><br/>";
                        message = t.getMessage();
                    }
                }
                ErrorWindow window = new ErrorWindow(intro, message);
                window.show();

                Throwable ex = event.getThrowable();
                if (ex != null) {
                    log.error(ex.getMessage(), ex);
                } else {
                    log.error("An unexpected error occurred");
                }
            }
        });

        VaadinSession.getCurrent().setConverterFactory(new DefaultConverterFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Converter<Date, ?> createDateConverter(Class<?> sourceType) {
                return super.createDateConverter(sourceType);
            }

            protected Converter<String, ?> createStringConverter(Class<?> sourceType) {
                if (Double.class.isAssignableFrom(sourceType)) {
                    return new StringToDoubleConverter();
                } else if (Float.class.isAssignableFrom(sourceType)) {
                    return new StringToFloatConverter();
                } else if (Integer.class.isAssignableFrom(sourceType)) {
                    return new StringToIntegerConverter() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected NumberFormat getFormat(Locale locale) {
                            NumberFormat format = super.getFormat(locale);
                            format.setGroupingUsed(false);
                            return format;
                        }
                    };
                } else if (Long.class.isAssignableFrom(sourceType)) {
                    return new StringToLongConverter() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected NumberFormat getFormat(Locale locale) {
                            NumberFormat format = super.getFormat(locale);
                            format.setGroupingUsed(false);
                            return format;
                        }
                    };
                } else if (BigDecimal.class.isAssignableFrom(sourceType)) {
                    return new StringToBigDecimalConverter();
                } else if (Boolean.class.isAssignableFrom(sourceType)) {
                    return new StringToBooleanConverter();
                } else if (Date.class.isAssignableFrom(sourceType)) {
                    return new StringToDateConverter() {
                        protected DateFormat getFormat(Locale locale) {
                            if (locale == null) {
                                locale = Locale.getDefault();
                            }
                            DateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            f.setLenient(false);
                            return f;
                        }
                    };
                } else {
                    return null;
                }
            }

        });

        Responsive.makeResponsive(this);
        ApplicationContext appCtx = ctx.getBean(ApplicationContext.class);
        IOperationsService operationsService = appCtx.getOperationsService();
        if (operationsService.isUserLoginEnabled()) {
            LoginDialog login = new LoginDialog(appCtx, this);
            UI.getCurrent().addWindow(login);
        } else {
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
        }
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
    public void detach() {
        if (backgroundRefresherService != null) {
            backgroundRefresherService.destroy();
        }
        if (appSession != null) {
            AppSession.remove(appSession);
        }
        super.detach();

    }

    @SuppressWarnings({ "serial" })
    class ErrorWindow extends ResizableWindow {
        public ErrorWindow(String intro, String message) {
            super("Error");
            setWidth(600f, Unit.PIXELS);
            setHeight(300f, Unit.PIXELS);
            content.setMargin(true);

            HorizontalLayout layout = new HorizontalLayout();
            Label icon = new Label();
            icon.setIcon(new ThemeResource("images/error.png"));
            icon.setWidth(70f, Unit.PIXELS);
            layout.addComponent(icon);

            Label labelIntro = new Label(intro, ContentMode.HTML);
            labelIntro.setStyleName("large");
            labelIntro.setWidth(530f, Unit.PIXELS);
            layout.addComponent(labelIntro);
            addComponent(layout);

            TextArea textField = new TextArea();
            textField.setSizeFull();
            textField.setWordwrap(false);
            textField.setValue(message);
            addComponent(textField);
            content.setExpandRatio(textField, 1.0f);

            addComponent(buildButtonFooter(buildCloseButton()));
        }
    }

    @Override
    public void login(User user) {
        
        appSession.setUser(user);
        AppSession.addAppSession(appSession);
        WebApplicationContext ctx = getWebApplicationContext();

        VerticalLayout root = new VerticalLayout();
        root.setSizeFull();
        setContent(root);

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();

        ApplicationContext appCtx = ctx.getBean(ApplicationContext.class);
        appCtx.setUser(user);
        
        appCtx.getConfigurationService().save(new AuditEvent(EventType.LOGIN, "Logged in", user.getLoginId()));
        user.setLastLoginTime(new Date());
        appCtx.getOperationsService().save(user);
        viewManager = getViewManager();
        viewManager.init(this, contentArea);

        TopBar menu = new TopBar(viewManager, appCtx);

        HorizontalLayout bottom = new HorizontalLayout();
        bottom.addStyleName(ValoTheme.LAYOUT_WELL);
        bottom.setWidth(100, Unit.PERCENTAGE);
        bottom.setMargin(new MarginInfo(false, true, false, true));

        HorizontalLayout left = new HorizontalLayout();
        left.setSpacing(true);

        Label logo = new Label("<a href='http://www.jumpmind.com'><img src='VAADIN/themes/apptheme/images/powered-by-jumpmind.png'/></a>",
                ContentMode.HTML);
        bottom.addComponents(logo);

        Label version = new Label("version " + VersionUtils.getCurrentVersion());
        left.addComponent(version);

        bottom.addComponent(left);
        bottom.setComponentAlignment(left, Alignment.MIDDLE_RIGHT);

        root.addComponents(menu, contentArea, bottom);
        root.setExpandRatio(contentArea, 1);
    }

    protected ViewManager getViewManager() {
        return getWebApplicationContext().getBean(ViewManager.class);
    }
}
