package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ComponentData;

public class Component extends AbstractObject<ComponentData> {

    public Component() {
        super(new ComponentData());
    }

    public Component(ComponentData data) {
        super(data);
    }
        
}
