package org.jumpmind.symmetric.is.ui.support;

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;

public final class UiUtils {

    private UiUtils() {
    }
    
    public static void notify(String message) {
        notify(null, message, Type.HUMANIZED_MESSAGE);
    }
    
    public static void notify(String caption, String message) {
        notify(caption, message, Type.HUMANIZED_MESSAGE);
    }

    public static void notify(String message, Type type) {
        notify(null, message, type);
    }
    
    public static void notify(String caption, String message, Type type) {
        Notification notification = new Notification(caption, message, type);
        notification.setPosition(Position.TOP_CENTER);
        notification.setStyleName(notification.getStyleName() + " " + ValoTheme.NOTIFICATION_BAR + " " + ValoTheme.NOTIFICATION_CLOSABLE);
        notification.show(Page.getCurrent());
    }

    public static void notify(Throwable ex) {
        notify("An unexpected error occurred", "The message was: " + ex.getMessage()
                + ".  See the log file for additional details", Type.ERROR_MESSAGE);
    }

}
