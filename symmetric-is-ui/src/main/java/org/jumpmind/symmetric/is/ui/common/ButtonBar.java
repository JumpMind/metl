package org.jumpmind.symmetric.is.ui.common;

import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;

public class ButtonBar extends HorizontalLayout {

    private static final long serialVersionUID = 1L;
    
    static final String STYLE = "button-bar";
    
    HorizontalLayout bar;

    public ButtonBar() {
        setWidth(100, Unit.PERCENTAGE);        
        setMargin(new MarginInfo(true, false, true, false));

        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setWidth(100, Unit.PERCENTAGE);
        wrapper.addStyleName(STYLE);

        bar = new HorizontalLayout();
        
        wrapper.addComponent(bar);
        
        addComponent(wrapper);
    }
    
    public Button addButton(String name, Resource icon) {
        Button button = new Button(name);
        button.addStyleName(STYLE);
        button.setIcon(icon);
        bar.addComponent(button);
        return button;
    }
}
