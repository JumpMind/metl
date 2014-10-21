package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;

public class ComponentFlow extends AbstractObject<ComponentFlowData> {

    private static final long serialVersionUID = 1L;
    
    List<ComponentFlowVersion> componentFlowVersions;
    
    public ComponentFlow() {
        this(new ComponentFlowData());
    }

    public ComponentFlow(ComponentFlowData data) {
        super(data);
        componentFlowVersions = new ArrayList<ComponentFlowVersion>();
    }
    
    public List<ComponentFlowVersion> getComponentFlowVersions() {
        return componentFlowVersions;
    }
    
    @Override
    public String toString() {
        return getData().getName();
    }

}
