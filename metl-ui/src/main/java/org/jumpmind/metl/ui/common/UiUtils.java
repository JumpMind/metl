package org.jumpmind.metl.ui.common;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

public final class UiUtils {

    private UiUtils() {
    }

    public static Label getName(String filter, String name) {
        if (filter != null && isNotBlank(filter)) {
            int[] startEndIndex = getFilterMatchRange(filter, name);
            if (startEndIndex[0] != -1) {
                String pre = startEndIndex[0] < name.length() ? name.substring(0, startEndIndex[0]) : "";
                String highlighted = name.substring(startEndIndex[0], startEndIndex[1]);
                String post = startEndIndex[1] < name.length() ? name.substring(startEndIndex[1]) : "";
                name = pre + "<span class='highlight'>" + highlighted + "</span>" + post;
            }
        }
        Label label = new Label(name, ContentMode.HTML);
        return label;
    }

    public static boolean filterMatches(String needle, String haystack) {
        return getFilterMatchRange(needle, haystack)[0] != -1;
    }

    public static int[] getFilterMatchRange(String needle, String haystack) {
        int startIndex = -1;
        int endIndex = 0;
        if (needle != null && isNotBlank(needle)) {
            needle = needle.toLowerCase();
            haystack = haystack.toLowerCase();
            for (String filterStr : needle.split("\\*")) {
                endIndex = haystack.indexOf(filterStr, endIndex);
                if (endIndex == -1) {
                    startIndex = -1;
                    break;
                } else {
                    if (startIndex == -1) {
                        startIndex = endIndex;
                    }
                    endIndex += filterStr.length();
                }
            }
        } else {
            startIndex = 0;
            endIndex = haystack.length();
        }
        return new int[] { startIndex, endIndex };
    }

}
