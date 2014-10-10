package org.jumpmind.symmetric.is.core.runtime.connection;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.Connection;

public interface IConnectionFactory {

    public IConnection create(Connection connection);

    public void register(Class<? extends IConnection> clazz);

    public List<String> getConnectionTypes();

}
