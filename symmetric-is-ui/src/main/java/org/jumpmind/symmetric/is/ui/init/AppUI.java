package org.jumpmind.symmetric.is.ui.init;

import org.jumpmind.symmetric.is.ui.common.DesignAgentSelect;
import org.jumpmind.symmetric.is.ui.common.TopBar;
import org.jumpmind.symmetric.is.ui.common.ViewManager;
import org.jumpmind.symmetric.ui.common.AbstractSpringUI;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.VerticalLayout;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
public class AppUI extends AbstractSpringUI {

    private static final long serialVersionUID = 1L;

    ViewManager viewManager;

    @Override
    protected void init(VaadinRequest request) {

        super.init(request);
        
        VerticalLayout root = new VerticalLayout();
        root.setSizeFull();
        setContent(root);

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();

        WebApplicationContext ctx = getWebApplicationContext();
        viewManager = ctx.getBean(ViewManager.class);
        viewManager.init(this, contentArea);
        
        DesignAgentSelect designAgentSelect = ctx.getBean(DesignAgentSelect.class);

        TopBar menu = new TopBar(viewManager, designAgentSelect);

        root.addComponents(menu, contentArea);
        root.setExpandRatio(contentArea, 1);

    }

}
