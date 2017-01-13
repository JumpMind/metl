package org.jumpmind.metl.core.runtime.resource;

import org.jumpmind.db.model.Table;

public interface IDatasourceRuntime extends IResourceRuntime {

    public void putTableInCache(String catalogName, String schemaName, String tableName, Table table);
    
    public Table getTableFromCache(String catalogName, String schemaName, String tableName);
    
}
