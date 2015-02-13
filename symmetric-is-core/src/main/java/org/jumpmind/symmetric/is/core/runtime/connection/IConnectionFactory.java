package org.jumpmind.symmetric.is.core.runtime.connection;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;

public interface IConnectionFactory {

    public IConnection create(Connection connection);

    public void register(Class<? extends IConnection> clazz);

    public List<String> getConnectionTypes();
    
    public List<String> getConnectionTypes(ConnectionCategory category);
    
    public Map<String, SettingDefinition> getSettingDefinitionsForConnectionType(String connectionType);

}
