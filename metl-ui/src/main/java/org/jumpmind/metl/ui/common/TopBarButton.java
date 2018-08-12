package org.jumpmind.metl.ui.common;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;

public class TopBarButton extends Button {

    private static final long serialVersionUID = 1L;

    public TopBarButton(String caption, Resource icon) {
        super(caption, icon);
    }

    public TopBarButton(Resource icon) {
        super(icon);
    }

}
