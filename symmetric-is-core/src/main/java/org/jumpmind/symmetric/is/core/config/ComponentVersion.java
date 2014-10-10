package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;

public class ComponentVersion extends AbstractObject<ComponentVersionData> {

    private static final long serialVersionUID = 1L;

    Connection connection;
    
    Component component;
    
    public ComponentVersion() {
    }
    
    public ComponentVersion(Component component, ComponentVersionData data) {
        super(data);
    }

    public Connection getConnection() {
        return connection;
    }
    
    public Component getComponent() {
        return component;
    }
    
}
