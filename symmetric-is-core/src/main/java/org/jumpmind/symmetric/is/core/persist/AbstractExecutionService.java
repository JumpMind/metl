package org.jumpmind.symmetric.is.core.persist;

import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.Execution;

abstract public class AbstractExecutionService extends AbstractService implements IExecutionService {
    
    public AbstractExecutionService(IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
    }

    @Override
    public void saveExecutionStatus(Execution execution) {
    }

}
