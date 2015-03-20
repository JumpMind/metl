package org.jumpmind.symmetric.is.ui.views;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalLayout;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.DEPLOY, name = "Deploy", id = "deploy", icon = FontAwesome.GEARS, menuOrder = 10)
public class DeployView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    static final FontAwesome DEPLOYMENT_ICON = FontAwesome.CUBE;


    public DeployView() {
        setSizeFull();
    }

    @PostConstruct
    protected void init() {
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
