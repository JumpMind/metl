package org.jumpmind.symmetric.is.core.model;


public class Setting extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    String name;
    String value;
    
    public Setting() {
    }
    
    public Setting(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "setting {" + name + ":" + value + "}";
    }
    
}
