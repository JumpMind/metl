package org.jumpmind.symmetric.is.ui.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class ResizableWindow extends Window {

    private static final long serialVersionUID = 1L;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public ResizableWindow() {
        setModal(true);
        setResizable(true);
        addShortcutListener(new ShortcutListener("Maximize", KeyCode.M,
                new int[] { ModifierKey.CTRL }) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (ResizableWindow.this.getWindowMode() != WindowMode.MAXIMIZED) {
                    ResizableWindow.this.setWindowMode(WindowMode.MAXIMIZED);
                } else {
                    ResizableWindow.this.setWindowMode(WindowMode.NORMAL);
                }
            }
        });
        addShortcutListener(new ShortcutListener("Close", KeyCode.ESCAPE, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                close();
            }
        });
    }

    protected Button buildCloseButton() {
        Button closeButton = new Button("Close");
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        closeButton.addClickListener(new CloseButtonListener());
        return closeButton;
    }

    protected HorizontalLayout buildButtonFooter(Button[] toTheLeftButtons, Button[] toTheRightButtons) {
        HorizontalLayout footer = new HorizontalLayout();

        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);

        if (toTheLeftButtons != null) {
            footer.addComponents(toTheLeftButtons);
        }
        
        Label footerText = new Label("");
        footerText.setSizeUndefined();

        footer.addComponents(footerText);
        footer.setExpandRatio(footerText, 1);

        if (toTheRightButtons != null) {
            footer.addComponents(toTheRightButtons);
        }


        return footer;
    }

    protected void grabFocus() {
        this.focus();
    }

    protected void resize(double percentOfBrowserSize, boolean showWindow) {
        Page page = Page.getCurrent();

        setWindowMode(WindowMode.NORMAL);

        int pageHeight = page.getBrowserWindowHeight();
        int pageWidth = page.getBrowserWindowWidth();

        setHeight((int) (pageHeight * percentOfBrowserSize), Unit.PIXELS);
        setWidth((int) (pageWidth * percentOfBrowserSize), Unit.PIXELS);

        int y = pageHeight / 2 - (int) getHeight() / 2;
        int x = pageWidth / 2 - (int) getWidth() / 2;

        setPositionX(x);
        setPositionY(y);

        if (showWindow && !UI.getCurrent().getWindows().contains(this)) {
            UI.getCurrent().addWindow(this);
            grabFocus();
        }
    }

    public class CloseButtonListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            close();
        }

    }

}
