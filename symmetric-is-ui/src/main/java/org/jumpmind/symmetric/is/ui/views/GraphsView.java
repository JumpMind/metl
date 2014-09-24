package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.VerticalLayout;

@Component
@ViewLink(category = Category.INTEGRATIONS, name = "Graphs", id = "graphs", icon = FontAwesome.SHARE_ALT, menuOrder = 10)
public class GraphsView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
