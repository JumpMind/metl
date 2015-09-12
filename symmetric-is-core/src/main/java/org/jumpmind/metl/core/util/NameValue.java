package org.jumpmind.symmetric.is.core.util;

import java.util.HashMap;

public class NameValue extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    String name;
    
    Object value;
    
    public NameValue(String name, Object value) {
        super(1);
        this.name = name;
        this.value = value;
        put(name, value);
    }
    
    public String getName() {
        return name;
    }
    
    public Object getValue() {
        return value;
    }    
}
