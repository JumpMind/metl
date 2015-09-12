package org.jumpmind.symmetric.is.ui.views;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.ui.common.AppConstants;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.DEPLOY, name = "Deploy", id = "deploy", icon = FontAwesome.GEARS, menuOrder = 10)
public class DeployView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    static final FontAwesome DEPLOYMENT_ICON = FontAwesome.CUBE;

    @Autowired
    ApplicationContext context;

    DeployNavigator deployNavigator;

    TabbedPanel tabbedPanel;

    @PostConstruct
    protected void init() {
        setSizeFull();

        tabbedPanel = new TabbedPanel();

        HorizontalSplitPanel leftSplit = new HorizontalSplitPanel();
        leftSplit.setSizeFull();
        leftSplit.setSplitPosition(AppConstants.DEFAULT_LEFT_SPLIT, Unit.PIXELS);

        deployNavigator = new DeployNavigator(context, tabbedPanel);

        leftSplit.setFirstComponent(deployNavigator);
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabbedPanel);
        leftSplit.setSecondComponent(container);

        addComponent(leftSplit);

    }

    @Override
    public void enter(ViewChangeEvent event) {
        deployNavigator.refresh();
    }

}
