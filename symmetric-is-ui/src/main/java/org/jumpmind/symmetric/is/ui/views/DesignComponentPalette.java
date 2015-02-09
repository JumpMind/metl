package org.jumpmind.symmetric.is.ui.views;

import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DesignComponentPalette extends Panel {

    private static final long serialVersionUID = 1L;
    
    public DesignComponentPalette() {
        setCaption("Component Palette");
        setSizeFull();
        addStyleName("noborder");
        VerticalLayout content = new VerticalLayout();
        setContent(content);
        
        addStyleName(ValoTheme.MENU_ROOT);

    }

}
