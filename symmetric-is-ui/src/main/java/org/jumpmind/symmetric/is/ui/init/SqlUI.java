package org.jumpmind.symmetric.is.ui.init;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

@Theme("apptheme")
@Title("Sql Explorer")
@PreserveOnRefresh
@Push(transport=Transport.WEBSOCKET)
public class SqlUI extends UI {

    private static final long serialVersionUID = 1L;

    @Override
    protected void init(VaadinRequest request) {
    }

}
