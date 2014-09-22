package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

public interface IPersistenceManager {
    
    public <T> List<T> find(Class<T> clazz, String catalogName, String schemaName, String tableName);

    public abstract boolean save(Object object, String catalogName, String schemaName, String tableName);

    public abstract boolean save(Object object);

    public abstract boolean delete(Object object, String catalogName, String schemaName, String tableName);

    public abstract boolean delete(Object object);
    
    public abstract void insert(Object object, String catalogName, String schemaName, String tableName);

    public abstract int update(Object object, String catalogName, String schemaName, String tableName);

}
