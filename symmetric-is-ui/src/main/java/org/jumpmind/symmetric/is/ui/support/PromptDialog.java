package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.Serializable;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class PromptDialog extends Window {

    private static final long serialVersionUID = 1L;

    public PromptDialog(String caption, String text, final IPromptListener iPromptListener) {
        this(caption, text, null, iPromptListener);
    }

    public PromptDialog(String caption, String text, String defaultValue,
            final IPromptListener promptListener) {
        setCaption(caption);
        setModal(true);
        setResizable(false);
        setSizeUndefined();
        setClosable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        if (isNotBlank(text)) {
            layout.addComponent(new Label(text));
        }

        final TextField field = new TextField();
        field.setWidth(100, Unit.PERCENTAGE);
        field.setNullRepresentation("");
        field.setValue(defaultValue);
        if (defaultValue != null) {
            field.setSelectionRange(0, defaultValue.length());
        }
        layout.addComponent(field);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);

        Label spacer = new Label(" ");
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);

        Button cancelButton = new Button("Cancel");
        cancelButton.setClickShortcut(KeyCode.ESCAPE);
        cancelButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                UI.getCurrent().removeWindow(PromptDialog.this);
            }
        });
        buttonLayout.addComponent(cancelButton);

        Button okButton = new Button("Ok");
        okButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        okButton.setClickShortcut(KeyCode.ENTER);
        okButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                if (promptListener.onOk(field.getValue())) {
                    UI.getCurrent().removeWindow(PromptDialog.this);
                }
            }
        });
        buttonLayout.addComponent(okButton);

        layout.addComponent(buttonLayout);

        field.focus();

    }

    public static void prompt(String caption, String message, IPromptListener listener) {
        PromptDialog prompt = new PromptDialog(caption, message, listener);
        UI.getCurrent().addWindow(prompt);
    }

    public static interface IPromptListener extends Serializable {
        public boolean onOk(String content);
    }

}
