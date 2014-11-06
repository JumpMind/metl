package org.jumpmind.symmetric.is.core.util;

import java.util.HashMap;

public class NameValue extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public NameValue(String name, Object value) {
        super(1);
        put(name, value);
    }
}
