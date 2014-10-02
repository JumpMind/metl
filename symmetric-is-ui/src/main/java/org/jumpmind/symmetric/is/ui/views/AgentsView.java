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
@Scope(value="ui")
@ViewLink(category = Category.RUNTIME, name = "Agents", id = "agents", icon = FontAwesome.GEARS, menuOrder = 10)
public class AgentsView extends AbstractFolderEditPanel implements View {

    private static final long serialVersionUID = 1L;    
    
    public AgentsView() {
        super("Agents", FolderType.AGENT);
    }
    
    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }
    

}
