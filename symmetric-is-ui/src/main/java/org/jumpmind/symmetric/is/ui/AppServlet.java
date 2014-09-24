package org.jumpmind.symmetric.is.ui;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinServlet;

@VaadinServletConfiguration(productionMode = false, ui = AppUI.class, widgetset = "org.jumpmind.symmetric.is.ui.AppWidgetSet")
public class AppServlet extends VaadinServlet {

    private static final long serialVersionUID = 1L;

    protected static final Logger log = LoggerFactory.getLogger(AppServlet.class);

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        getService().addSessionInitListener(new AppSessionInitListener());
    }


}