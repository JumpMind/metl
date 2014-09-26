package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.VerticalLayout;

@Component
@Scope(value="ui")
@ViewLink(category = Category.SHARED, name = "Models", id = "models", icon = FontAwesome.SITEMAP, menuOrder = 30)
public class ModelView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
