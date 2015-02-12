package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;

public class ComponentFlow extends AbstractObject<ComponentFlowData> {

    private static final long serialVersionUID = 1L;
    
    List<ComponentFlowVersion> componentFlowVersions;
    
    Folder folder;

    public ComponentFlow(Folder folder, ComponentFlowData data) {
        super(data);
        componentFlowVersions = new ArrayList<ComponentFlowVersion>();
    }
    
    public List<ComponentFlowVersion> getComponentFlowVersions() {
        return componentFlowVersions;
    }
    
    public Folder getFolder() {
        return folder;
    }
    
    public void setName(String name) {
        this.data.setName(name);
    }
    
    public String getName() {
        return this.data.getName();
    }
    
    @Override
    public String toString() {
        return getData().getName();
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

}
