package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Execution;
import org.jumpmind.symmetric.is.core.config.ExecutionStatus;

public class ExecutionSqlService extends AbstractExecutionService {

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

    @Override
    public List<Execution> findActiveExecutions(Agent agent) {
        return null;
    }
}
