package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionSettingData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class ComponentVersion extends AbstractObjectWithSettings<ComponentVersionData> {

    private static final long serialVersionUID = 1L;

    Connection connection;
    
    Component component;
    
    public ComponentVersion(Component component, Connection connection, ComponentVersionData data, SettingData... settings) {
        super(data, settings);
        this.component = component;
        this.connection = connection;
        data.setComponentId(component.getData().getId());
        if (connection != null) {
            data.setConnectionId(connection.getData().getId());
        }
    }

    public String getName() {
        return data.getName();
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public Component getComponent() {
        return component;
    }
    
    @Override
    protected SettingData createSettingData() {
        return new ComponentVersionSettingData(data.getId());
    }
    
}
