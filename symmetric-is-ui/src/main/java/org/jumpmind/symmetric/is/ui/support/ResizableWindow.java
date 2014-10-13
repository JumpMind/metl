package org.jumpmind.symmetric.is.ui.support;

import org.jumpmind.symmetric.is.ui.support.ConfirmDialog.IConfirmListener;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class ResizableWindow extends Window {

    private static final long serialVersionUID = 1L;

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
                closeNoSave();
            }
        });
    }

    @Override
    public void close() {
        closeNoSave();
    }

    protected void closeNoSave() {
        ConfirmDialog.show("Close Window?", "Are you sure you want to close this window?",
                new IConfirmListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean onOk() {
                        UI.getCurrent().removeWindow(ResizableWindow.this);
                        return true;
                    }
                });
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
        }
    }

    public class CancelButtonListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            closeNoSave();
        }
        
    }

}
