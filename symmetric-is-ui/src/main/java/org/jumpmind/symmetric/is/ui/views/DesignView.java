package org.jumpmind.symmetric.is.ui.views;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
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
import com.vaadin.ui.VerticalSplitPanel;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.DESIGN, name = "Design", id = "design", icon = FontAwesome.SHARE_ALT, menuOrder = 1, useAsDefault = true)
public class DesignView extends HorizontalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IComponentFactory componentFactory;

    @Autowired
    IResourceFactory resourceFactory;
    
    @Autowired
    BackgroundRefresherService backgroundRefresherService;

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

        VerticalSplitPanel leftTopBottomSplit = new VerticalSplitPanel();
        leftTopBottomSplit.setSizeFull();
        leftTopBottomSplit.setSplitPosition(60, Unit.PERCENTAGE);

        designPropertySheet = new DesignPropertySheet(componentFactory, configurationService,
                resourceFactory);

        DesignComponentPalette designComponentPalette = new DesignComponentPalette(componentFactory, leftTopBottomSplit);
        designNavigator = new DesignNavigator(backgroundRefresherService, configurationService, tabs,
                designComponentPalette, designPropertySheet);
        leftTopBottomSplit.setFirstComponent(designNavigator);
        leftTopBottomSplit.setSecondComponent(designComponentPalette);

        leftSplit.setFirstComponent(leftTopBottomSplit);
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
