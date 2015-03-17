package org.jumpmind.symmetric.is.ui.views;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.TabbedApplicationPanel;
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
@TopBarLink(category = Category.DESIGN, name = "Design", id = "design", icon = FontAwesome.SHARE_ALT, menuOrder = 1, useAsDefault = true)
public class DesignView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    DesignNavigator designNavigator;

    TabbedApplicationPanel tabs;

    @PostConstruct
    protected void init() {
        setSizeFull();

        tabs = new TabbedApplicationPanel();


        HorizontalSplitPanel leftSplit = new HorizontalSplitPanel();
        leftSplit.setSizeFull();
        leftSplit.setSplitPosition(300, Unit.PIXELS);

        designNavigator = new DesignNavigator(context, tabs);

        leftSplit.setFirstComponent(designNavigator);
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabs);
        leftSplit.setSecondComponent(container);

        addComponent(leftSplit);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        designNavigator.refresh();
    }

}
