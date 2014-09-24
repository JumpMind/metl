package org.jumpmind.symmetric.is.ui.support;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class PageNotFoundView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;
    Label pageNotFoundLabel = new Label();

    public PageNotFoundView() {
        setSizeFull();
        setMargin(true);
        addComponent(pageNotFoundLabel);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        pageNotFoundLabel.addStyleName("failure");
        pageNotFoundLabel.setValue("Could not find page for "
                + Page.getCurrent().getUriFragment());
    }

}
