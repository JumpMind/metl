package org.jumpmind.symmetric.is.core.persist;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
            inClause.append("'").append(executionStepId).append("'");
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
    
    public List<Execution> findExecutions(Map<String, Object> params, int limit) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        StringBuilder whereClause = new StringBuilder();
        int i = params.size();
        for (String columnName : params.keySet()) {
        	whereClause.append(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(columnName), "_")).append(" = ? ");
        	if (--i > 0) {
        		whereClause.append("and ");
        	}
        }
        return template.query(String.format(
        		"select id, agent_id, flow_version_id, agent_name, host_name, flow_name, status, start_time, end_time " +
                "from %1$s_execution " +
                "where " + whereClause + "order by create_time limit " + limit,
                tablePrefix), new ISqlRowMapper<Execution>() {
                    public Execution mapRow(Row row) {
                    	Execution e = new Execution();
                    	e.setId(row.getString("id"));
                    	e.setAgentId(row.getString("agent_id"));
                    	e.setFlowVersionId(row.getString("flow_version_id"));
                    	e.setAgentName(row.getString("agent_name"));
                    	e.setHostName(row.getString("host_name"));
                    	e.setFlowName(row.getString("flow_name"));
                    	e.setStatus(row.getString("status"));
                    	e.setStartTime(row.getDateTime("start_time"));
                    	e.setEndTime(row.getDateTime("end_time"));
                        return e;
                    }
                }, params.values().toArray());
    }

    @Override
    public List<Execution> findActiveExecutions(Agent agent) {
        return null;
    }
}
