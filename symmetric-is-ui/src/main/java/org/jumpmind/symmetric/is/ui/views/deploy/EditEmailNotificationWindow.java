package org.jumpmind.symmetric.is.ui.views.deploy;

import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.ResizableWindow;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditEmailNotificationWindow extends ResizableWindow {

    ApplicationContext context;

    public EditEmailNotificationWindow(ApplicationContext context) {
        super("Email Notifications");
        this.context = context;
        
        CheckBox startCheckbox = new CheckBox("Send notification when flow is started");
        addComponent(startCheckbox);

        CheckBox endCheckbox = new CheckBox("Send notification when flow is ended");
        addComponent(endCheckbox);

        CheckBox errorCheckbox = new CheckBox("Send notification when flow is in error");
        addComponent(errorCheckbox);

        TextArea textArea = new TextArea("Email recipients (separate using whitespace, comma, or semi-colon)");
        textArea.setColumns(30);
        textArea.setRows(10);
        addComponent(textArea);
        content.setExpandRatio(textArea, 1f);
        
        Button closeButton = new Button("Close");
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        closeButton.addClickListener(new CloseClickListener());

        content.setSpacing(true);
        content.setMargin(true);
        HorizontalLayout footer = buildButtonFooter(closeButton);
        addComponent(footer);
        content.setExpandRatio(footer, 0.2f);
    }

    class CloseClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            EditEmailNotificationWindow.this.close();
        }
    }

}
