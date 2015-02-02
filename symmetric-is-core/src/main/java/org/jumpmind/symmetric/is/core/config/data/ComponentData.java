package org.jumpmind.symmetric.is.core.config.data;

public class ComponentData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String type;
    
    boolean shared;

    String name;

    public ComponentData() {
    }
        
    public ComponentData(String type, boolean shared) {
        super();
        this.type = type;
        this.shared = shared;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }       

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

}
