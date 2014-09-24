package org.jumpmind.symmetric.is.ui;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
public class AppUI extends UI {

    private static final long serialVersionUID = 1L;

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = AppUI.class, widgetset = "org.jumpmind.symmetric.is.ui.AppWidgetSet")
    public static class Servlet extends VaadinServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void servletInitialized() throws ServletException {
            super.servletInitialized();
            getService().addSessionInitListener(new AppSessionInitListener());
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        setContent(new Button("Hi"));
    }

}
