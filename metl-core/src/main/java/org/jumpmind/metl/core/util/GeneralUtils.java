package org.jumpmind.metl.core.util;

final public class GeneralUtils {

    private GeneralUtils() {
    }

    public static String replaceSpecialCharacters(String value) {
        value = value.replaceAll("[\\s]", "_");
        value = value.replaceAll("[^a-zA-Z0-9_\\.]", "");
        return value;
    }

}
