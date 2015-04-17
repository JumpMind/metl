package org.jumpmind.symmetric.is.ui.common;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

public final class UiUtils {

    private UiUtils() {
    }

    public static Label getName(String filter, String name) {
        if (isNotBlank(filter)) {
            filter = filter.toLowerCase();
            if (name.toLowerCase().contains(filter)) {
                int start = name.toLowerCase().indexOf(filter);
                String pre = start < name.length() ? name.substring(0, start) : "";
                String highlighted = name.substring(start, start + filter.length());
                String post = start + filter.length() < name.length() ? name.substring(start
                        + filter.length()) : "";
                name = pre + "<span class='highlight'>" + highlighted + "</span>" + post;
            }
        }
        Label label = new Label(name, ContentMode.HTML);
        return label;
    }
}
