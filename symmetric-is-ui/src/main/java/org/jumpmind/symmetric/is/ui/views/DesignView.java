package org.jumpmind.symmetric.is.ui.views;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.TabbedApplicationPanel;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
import org.jumpmind.symmetric.is.ui.init.BackgroundRefresherService;
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
    IConfigurationService configurationService;

    @Autowired
    IExecutionService executionService;

    @Autowired
    IComponentFactory componentFactory;

    @Autowired
    IResourceFactory resourceFactory;
    
    @Autowired
    BackgroundRefresherService backgroundRefresherService;
    
    @Autowired
    IAgentManager agentManager;

    DesignNavigator designNavigator;

    DesignPropertySheet designPropertySheet;

    TabbedApplicationPanel tabs;

    @PostConstruct
    protected void init() {
        setSizeFull();

        tabs = new TabbedApplicationPanel();

        HorizontalSplitPanel rightSplit = new HorizontalSplitPanel();
        rightSplit.setSizeFull();
        rightSplit.setSplitPosition(300, Unit.PIXELS, true);

        HorizontalSplitPanel leftSplit = new HorizontalSplitPanel();
        leftSplit.setSizeFull();
        leftSplit.setSplitPosition(300, Unit.PIXELS);

        designPropertySheet = new DesignPropertySheet(componentFactory, configurationService,
                resourceFactory);

        designNavigator = new DesignNavigator(agentManager, backgroundRefresherService, configurationService, executionService, tabs,
                designPropertySheet, componentFactory, resourceFactory);

        leftSplit.setFirstComponent(designNavigator);
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabs);
        leftSplit.setSecondComponent(container);
        rightSplit.setFirstComponent(leftSplit);

        designNavigator.addValueChangeListener(designPropertySheet);

        rightSplit.setSecondComponent(designPropertySheet);

        addComponent(rightSplit);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        designNavigator.refresh();
    }

}
