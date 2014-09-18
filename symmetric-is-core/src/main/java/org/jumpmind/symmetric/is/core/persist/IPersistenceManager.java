package org.jumpmind.symmetric.is.core.persist;

public interface IPersistenceManager {

    public abstract void insert(Object object, String catalogName, String schemaName, String tableName);

    public abstract int update(Object object, String catalogName, String schemaName, String tableName);

    public abstract boolean save(Object object, String catalogName, String schemaName, String tableName);

    public abstract boolean save(Object object);

}
