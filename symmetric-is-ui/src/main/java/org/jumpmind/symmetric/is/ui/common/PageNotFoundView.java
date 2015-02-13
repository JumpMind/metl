package org.jumpmind.symmetric.is.ui.common;

import static org.apache.commons.lang.StringUtils.isBlank;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class PageNotFoundView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    Label pageNotFoundLabel = new Label();

    ViewManager viewManager;

    public PageNotFoundView(ViewManager viewManager) {
        this.viewManager = viewManager;
        setSizeFull();
        setMargin(true);
        addComponent(pageNotFoundLabel);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        String uriFragment = Page.getCurrent().getUriFragment();
        if (isBlank(uriFragment)) {
            viewManager.navigateToDefault();
        } else {
            pageNotFoundLabel.addStyleName("failure");
            pageNotFoundLabel.setValue("Could not find page for " + uriFragment);
        }
    }

}
