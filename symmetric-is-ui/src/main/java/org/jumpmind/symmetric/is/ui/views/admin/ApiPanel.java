package org.jumpmind.symmetric.is.ui.views.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.ui.BrowserFrame;

public class ApiPanel extends NamedPanel {

    final Log logger = LogFactory.getLog(getClass());

    private static final long serialVersionUID = 1L;

    public ApiPanel(ApplicationContext context, TabbedPanel tabbedPanel) {   
        super("API");
        setSizeFull();
        BrowserFrame e = new BrowserFrame(null, new ExternalResource(Page.getCurrent()
                .getLocation().getPath().replace("/app", "api.html")));
        e.setSizeFull();
        addComponent(e);
    }

}
