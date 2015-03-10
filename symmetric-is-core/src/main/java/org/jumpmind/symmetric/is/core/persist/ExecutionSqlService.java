package org.jumpmind.symmetric.is.core.persist;

import java.util.List;
import java.util.Set;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStatus;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;

public class ExecutionSqlService extends AbstractExecutionService implements IExecutionService {

    IDatabasePlatform databasePlatform;

    public ExecutionSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
    }

    @Override
    public Execution requestExecution(AgentDeployment deployment) {
        String sql = String
                .format("insert into %1$s_execution (id, agent_deployment_id, status, create_time, create_by, last_update_time, last_update_by) "
                        + "(select ?, ?, ?, ?, ?, ?, ? where not exists (select * from %1$s_execution where agent_deployment_id=? and end_time is null)) ",
                        tablePrefix);
        Execution execution = new Execution(ExecutionStatus.REQUESTED, deployment.getId());

        ISqlTemplate template = databasePlatform.getSqlTemplate();
        if (1 <= template.update(sql, execution.getId(), execution.getAgentDeploymentId(), execution.getStatus(),
                execution.getCreateTime(), execution.getCreateBy(), execution.getLastModifyTime(),
                execution.getLastModifyBy())) {
            return execution;
        } else {
            return null;
        }
    }

    public List<ExecutionStepLog> findExecutionStepLog(Set<String> executionStepIds) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        StringBuilder inClause = new StringBuilder("(");
        int i = executionStepIds.size();
        for (String executionStepId : executionStepIds) {
        	inClause.append(executionStepId);
        	if (i-- > 0) {
        		inClause.append(",");
        	}
        }
        return template.query(String.format(
        		"select id, execution_step_id, category, level, log_text, create_time " +
                "from %1$s_execution_step_log " +
                "where execution_step_id in " + inClause.toString() + " order by create_time",
                tablePrefix), new ISqlRowMapper<ExecutionStepLog>() {
                    @Override
                    public ExecutionStepLog mapRow(Row row) {
                    	ExecutionStepLog e = new ExecutionStepLog();
                    	e.setId(row.getString("id"));
                    	e.setExecutionStepId(row.getString("executionStepId"));
                    	e.setCategory(row.getString("category"));
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
