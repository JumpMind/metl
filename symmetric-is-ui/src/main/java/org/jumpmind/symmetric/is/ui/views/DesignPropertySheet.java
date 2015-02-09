package org.jumpmind.symmetric.is.ui.views;

import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DesignPropertySheet extends Panel {

    private static final long serialVersionUID = 1L;
    
    public DesignPropertySheet() {
        setCaption("Property Sheet");
        setSizeFull();
        addStyleName("noborder");
        VerticalLayout content = new VerticalLayout();
        setContent(content);
        
        addStyleName(ValoTheme.MENU_ROOT);

    }

}
