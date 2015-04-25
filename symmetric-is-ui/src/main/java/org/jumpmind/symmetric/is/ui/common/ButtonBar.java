package org.jumpmind.symmetric.is.ui.common;

import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

public class ButtonBar extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    static final String STYLE = "button-bar";

    HorizontalLayout bar;
    
    HorizontalLayout wrapper;

    public ButtonBar() {
        setWidth(100, Unit.PERCENTAGE);
        setMargin(new MarginInfo(true, false, true, false));

        wrapper = new HorizontalLayout();
        wrapper.setWidth(100, Unit.PERCENTAGE);
        wrapper.addStyleName(STYLE);
        wrapper.setMargin(new MarginInfo(false, true, false, false));

        bar = new HorizontalLayout();

        wrapper.addComponent(bar);
        
        Label spacer = new Label();
        spacer.addStyleName(STYLE);
        wrapper.addComponent(spacer);
        wrapper.setExpandRatio(spacer, 1);
        
        addComponent(wrapper);
    }
    
    public TextField addFilter() {
        TextField textField = new TextField();
        textField.setInputPrompt("Filter");
        textField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        textField.setTextChangeTimeout(500);
        addRight(textField);
        return textField;
    }
    
    public void addRight(Component component) {
        wrapper.addComponent(component);
        wrapper.setComponentAlignment(component, Alignment.MIDDLE_RIGHT);
    }

    public Button addButton(String name, Resource icon) {
        return addButton(name, icon, null);
    }    

    public Button addButton(String name, Resource icon, ClickListener clickListener) {
        Button button = new Button(name);
        button.addStyleName(STYLE);
        button.setIcon(icon);
        if (clickListener != null) {
            button.addClickListener(clickListener);
        }
        bar.addComponent(button);
        return button;
    }
    
}
