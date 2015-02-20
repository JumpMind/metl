package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class Component extends AbstractObject {

    private static final long serialVersionUID = 1L;

    List<ComponentVersion> componentVersions;

    String type;

    boolean shared;

    String name;

    public Component() {
        this.componentVersions = new ArrayList<ComponentVersion>();
    }

    public Component(String id) {
    	this();
    	this.id = id;
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

    public List<ComponentVersion> getComponentVersions() {
        return componentVersions;
    }

    public void setComponentVersions(List<ComponentVersion> componentVersions) {
        this.componentVersions = componentVersions;
    }

}
