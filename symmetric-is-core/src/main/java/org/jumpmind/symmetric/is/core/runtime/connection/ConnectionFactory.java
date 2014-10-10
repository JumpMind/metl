package org.jumpmind.symmetric.is.core.runtime.connection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionDefinition;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnection;

public class ConnectionFactory implements IConnectionFactory {

    
    Map<String, Class<? extends IConnection>> connectionTypes = new LinkedHashMap<String, Class<? extends IConnection>>();

    public ConnectionFactory() {
        register(DataSourceConnection.class);
    }

    @Override
    public List<String> getConnectionTypes() {
        return new ArrayList<String>(connectionTypes.keySet());
    }

    @Override
    public void register(Class<? extends IConnection> clazz) {
        ConnectionDefinition definition = clazz.getAnnotation(ConnectionDefinition.class);
        if (definition != null) {
            connectionTypes.put(definition.typeName(), clazz);
        } else {
            throw new IllegalStateException("A connection is required to define the "
                    + ConnectionDefinition.class.getName() + " annotation");
        }
    }

    @Override
    public IConnection create(Connection connection) {
        try {
            String connectionType = connection.getData().getType();
            Class<? extends IConnection> clazz = connectionTypes.get(connectionType);
            if (clazz != null) {
                return clazz.newInstance();
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
    
}
