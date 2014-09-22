package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ComponentGraphData;

public class ComponentGraph extends AbstractObject<ComponentGraphData> {

    List<ComponentGraphVersion> componentGraphVersions;
    
    public ComponentGraph() {
        this(new ComponentGraphData());
    }

    public ComponentGraph(ComponentGraphData data) {
        super(data);
        componentGraphVersions = new ArrayList<ComponentGraphVersion>();
    }

}
