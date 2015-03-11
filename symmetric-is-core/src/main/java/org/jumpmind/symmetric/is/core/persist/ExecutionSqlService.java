package org.jumpmind.symmetric.is.core.persist;

import java.util.List;
import java.util.Set;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;

public class ExecutionSqlService extends AbstractExecutionService implements IExecutionService {

    IDatabasePlatform databasePlatform;

    public ExecutionSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
    }

    public List<ExecutionStepLog> findExecutionStepLog(Set<String> executionStepIds) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        StringBuilder inClause = new StringBuilder("(");
        int i = executionStepIds.size();
        for (String executionStepId : executionStepIds) {
            inClause.append("'");
        	inClause.append(executionStepId);
        	inClause.append("'");
        	if (--i > 0) {
        		inClause.append(",");
        	}
        }
        inClause.append(")");
        return template.query(String.format(
        		"select id, execution_step_id, level, log_text, create_time " +
                "from %1$s_execution_step_log " +
                "where execution_step_id in " + inClause.toString() + " order by create_time",
                tablePrefix), new ISqlRowMapper<ExecutionStepLog>() {
                    @Override
                    public ExecutionStepLog mapRow(Row row) {
                    	ExecutionStepLog e = new ExecutionStepLog();
                    	e.setId(row.getString("id"));
                    	e.setExecutionStepId(row.getString("execution_step_id"));
                    	e.setLevel(row.getString("level"));
                    	e.setLogText(row.getString("log_text"));
                    	e.setCreateTime(row.getDateTime("create_time"));
                        return e;
                    }
                });    	
    }

    @Override
    public List<Execution> findActiveExecutions(Agent agent) {
        return null;
    }
}
