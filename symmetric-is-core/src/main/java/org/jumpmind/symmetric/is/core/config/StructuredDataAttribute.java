package org.jumpmind.symmetric.is.core.config;

public class StructuredDataAttribute {

    String id;
    
    DataType type;
    
    String name;

    public StructuredDataAttribute(DataType type, String name) {
        super();
        this.type = type;
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public DataType getType() {
        return type;
    }
    
}
