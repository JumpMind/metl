package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.ui.support.AbstractFolderNavigatorLayout;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.UiComponent;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;

@UiComponent
@Scope(value="ui")
@ViewLink(category = Category.RUNTIME, name = "Agents", id = "agents", icon = FontAwesome.GEARS, menuOrder = 10)
public class AgentsView extends AbstractFolderNavigatorLayout implements View {

    private static final long serialVersionUID = 1L;    
    
    public AgentsView() {
        super("Agents", FolderType.RUNTIME);
    }
    
    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }
    

}
