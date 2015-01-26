package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.Execution;

public class ExecutionSqlService extends AbstractExecutionService {
    
    String tablePrefix;

    IDatabasePlatform databasePlatform;

    public ExecutionSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager);
        this.databasePlatform = databasePlatform;
        this.tablePrefix = tablePrefix;
    }
    
    @Override
    public List<Execution> findActiveExecutions(Agent agent) {
        return null;
    }
}
