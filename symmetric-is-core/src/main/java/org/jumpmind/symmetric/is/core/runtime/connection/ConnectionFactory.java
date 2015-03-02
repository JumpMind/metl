package org.jumpmind.symmetric.is.core.runtime.connection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.connection.db.DataSourceConnection;
import org.jumpmind.symmetric.is.core.runtime.connection.localfile.DASNASConnection;

public class ConnectionFactory implements IConnectionFactory {

    Map<String, Class<? extends IConnection>> connectionTypes = new LinkedHashMap<String, Class<? extends IConnection>>();

    Map<ConnectionCategory, List<String>> categoryToTypeMapping = new LinkedHashMap<ConnectionCategory, List<String>>();

    public ConnectionFactory() {
        register(DataSourceConnection.class);
        register(DASNASConnection.class);
    }

    @Override
    public List<String> getConnectionTypes() {
        return new ArrayList<String>(connectionTypes.keySet());
    }
    
    @Override
    public List<String> getConnectionTypes(ConnectionCategory category) {
        return categoryToTypeMapping.get(category);
    }

    @Override
    public void register(Class<? extends IConnection> clazz) {
        ConnectionDefinition definition = clazz.getAnnotation(ConnectionDefinition.class);
        if (definition != null) {
            connectionTypes.put(definition.typeName(), clazz);
            List<String> types = categoryToTypeMapping.get(definition.connectionCategory());
            if (types == null) {
                types = new ArrayList<String>();
                categoryToTypeMapping.put(definition.connectionCategory(), types);
            }
            types.add(definition.typeName());
        } else {
            throw new IllegalStateException("A connection is required to define the "
                    + ConnectionDefinition.class.getName() + " annotation");
        }
    }

    @Override
    public IConnection create(Connection connection) {
        try {
            String connectionType = connection.getType();
            Class<? extends IConnection> clazz = connectionTypes.get(connectionType);
            if (clazz != null) {
                IConnection runtime = clazz.newInstance();
                runtime.start(connection);
                return runtime;
            } else {
                throw new IllegalStateException(
                        "Could not find a class associated with the connection type of "
                                + connectionType);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Map<String, SettingDefinition> getSettingDefinitionsForConnectionType(
            String connectionType) {
        Class<? extends IConnection> clazz = connectionTypes.get(connectionType);
        return AbstractRuntimeObject.getSettingDefinitions(clazz, false);
    }

}
