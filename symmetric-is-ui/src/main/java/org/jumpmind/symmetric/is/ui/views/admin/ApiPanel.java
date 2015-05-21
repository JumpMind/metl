package org.jumpmind.symmetric.is.ui.views.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.VerticalLayout;

public class ApiPanel extends VerticalLayout implements IUiPanel {

    final Log logger = LogFactory.getLog(getClass());

    private static final long serialVersionUID = 1L;

    public ApiPanel(ApplicationContext context, TabbedPanel tabbedPanel, String caption, Resource icon) {   
        setSizeFull();
        BrowserFrame e = new BrowserFrame(null, new ExternalResource(Page.getCurrent()
                .getLocation().getPath().replace("/app", "api.html")));
        e.setSizeFull();
        addComponent(e);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

}
