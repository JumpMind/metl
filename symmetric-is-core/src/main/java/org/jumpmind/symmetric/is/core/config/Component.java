package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentData;

public class Component extends AbstractObject<ComponentData> {

    private static final long serialVersionUID = 1L;
    List<ComponentVersion> componentVersions;
    
    public Component() {
        super(new ComponentData());
    }
    

    public String getName() {
        return data.getName();
    }
    
    public void setName(String name) {
        this.data.setName(name);
    }

    public Component(ComponentData data) {
        super(data);
        this.componentVersions = new ArrayList<ComponentVersion>();
    }
    
    public String getType() {
        return data.getType();
    }

    public boolean isShared() {
        return data.isShared();
    }
    
    public List<ComponentVersion> getComponentVersions() {
        return componentVersions;
    }
    
    public void setComponentVersions(List<ComponentVersion> componentVersions) {
        this.componentVersions = componentVersions;
    }
        
}
