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
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Component
@Scope(value = "ui")
@ViewLink(category = Category.SHARED, name = "Connections", id = "connections", icon = FontAwesome.LINK, menuOrder = 10)
public class ConnectionsView extends AbstractFolderEditPanel implements View {

    private static final long serialVersionUID = 1L;

    Button addConnectionButton;
    
    Button delConnectionButton;
    
    public ConnectionsView() {
        super("Connections", FolderType.CONNECTION);
    }

    @Override
    protected void addButtonsRight(HorizontalLayout buttonLayout) {
        addConnectionButton = new Button("Add");
        addConnectionButton.setIcon(FontAwesome.LINK);
        addConnectionButton.setStyleName(ValoTheme.BUTTON_LINK);
        buttonLayout.addComponent(addConnectionButton);
        buttonLayout.setComponentAlignment(addConnectionButton, Alignment.MIDDLE_LEFT);
        
        delConnectionButton = new Button("Delete");
        delConnectionButton.setIcon(FontAwesome.LINK);
        delConnectionButton.setStyleName(ValoTheme.BUTTON_LINK);
        buttonLayout.addComponent(delConnectionButton);
        buttonLayout.setComponentAlignment(delConnectionButton, Alignment.MIDDLE_LEFT);

    }
    
    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

}
