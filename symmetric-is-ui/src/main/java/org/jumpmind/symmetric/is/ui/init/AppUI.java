package org.jumpmind.symmetric.is.ui.init;

import org.jumpmind.symmetric.is.ui.support.ViewManager;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
@Push
public class AppUI extends UI {

    private static final long serialVersionUID = 1L;

    ViewManager viewManager;

    static {
        SLF4JBridgeHandler.install();
    }

    @Override
    protected void init(VaadinRequest request) {

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
