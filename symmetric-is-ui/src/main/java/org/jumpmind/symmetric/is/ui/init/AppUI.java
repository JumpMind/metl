package org.jumpmind.symmetric.is.ui.init;

import org.jumpmind.symmetric.is.ui.support.Menu;
import org.jumpmind.symmetric.is.ui.support.ViewManager;
import org.jumpmind.symmetric.ui.common.AbstractSpringUI;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
@Push(transport=Transport.WEBSOCKET)
public class AppUI extends AbstractSpringUI {

    private static final long serialVersionUID = 1L;

    ViewManager viewManager;

    @Override
    protected void init(VaadinRequest request) {

        super.init(request);
        
        HorizontalLayout root = new HorizontalLayout();
        root.setSizeFull();
        setContent(root);

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

}
