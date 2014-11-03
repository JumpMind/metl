package org.jumpmind.symmetric.is.ui.init;

import org.jumpmind.symmetric.is.ui.support.Menu;
import org.jumpmind.symmetric.is.ui.support.UiUtils;
import org.jumpmind.symmetric.is.ui.support.ViewManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
@Push(transport=Transport.WEBSOCKET)
public class AppUI extends UI {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AppUI.class);

    ViewManager viewManager;

    @Override
    protected void init(VaadinRequest request) {

        setErrorHandler(new DefaultErrorHandler() {

            private static final long serialVersionUID = 1L;

            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                Throwable ex = event.getThrowable();
                UiUtils.notify("Error",
                        "An unexpected error occurred.  Please check the log file for details.",
                        Type.ERROR_MESSAGE);
                if (ex != null) {
                    log.error(ex.getMessage(), ex);
                } else {
                    log.error("An unexpected error occurred");
                }
            }
        });

        HorizontalLayout root = new HorizontalLayout();
        root.setSizeFull();
        setContent(root);

        Responsive.makeResponsive(this);

        CssLayout contentArea = new CssLayout();
        contentArea.setPrimaryStyleName("valo-content");
        contentArea.addStyleName("v-scrollable");
        contentArea.setSizeFull();

        viewManager = getWebApplicationContext().getBean(ViewManager.class);
        viewManager.init(this, contentArea);

        Menu menu = new Menu(viewManager);

        root.addComponents(menu, contentArea);
        root.setExpandRatio(contentArea, 1);

    }

    public WebApplicationContext getWebApplicationContext() {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(VaadinServlet
                .getCurrent().getServletContext());
    }

}
