package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.ui.support.AbstractFolderEditPanel;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;

@Component
@Scope(value = "ui")
@ViewLink(category = Category.SHARED, name = "Connections", id = "connections", icon = FontAwesome.LINK, menuOrder = 10)
public class ConnectionsView extends AbstractFolderEditPanel implements View {

    private static final long serialVersionUID = 1L;

    public ConnectionsView() {
        super("Connections", FolderType.CONNECTION);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

}
