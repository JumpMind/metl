/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.persist;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.sql.mapper.StringMapper;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.persist.IPersistenceManager;
import org.springframework.core.env.Environment;

public class ExecutionSqlService extends AbstractExecutionService implements IExecutionService {

    IDatabasePlatform databasePlatform;

    public ExecutionSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix, Environment env) {
        super(persistenceManager, tablePrefix, env);
        this.databasePlatform = databasePlatform;
    }
    
    public void markAbandoned(String agentId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        int count = template.update(
                String.format(
                        "update %1$s_execution_step set status=? where (status=? or status=?) and execution_id in (select execution_id from %1$s_execution where agent_id=?)",
                        tablePrefix), ExecutionStatus.ABANDONED.name(), ExecutionStatus.RUNNING
                        .name(), ExecutionStatus.READY.name(), agentId);
        if (count > 0) {
            log.info("Updated {} execution step records that were abandoned", count);
        }
        count = template.update(
                String.format(
                        "update %1$s_execution set status=? where (status=? or status=?) and agent_id=?",
                        tablePrefix), ExecutionStatus.ABANDONED.name(), ExecutionStatus.RUNNING
                        .name(), ExecutionStatus.READY.name(), agentId);
        if (count > 0) {
            log.info("Updated {} execution records that were abandoned", count);
        }
    }
    
    public List<String> findExecutedFlowIds () {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.query(String.format(
                "select distinct flow_id from %1$s_execution", tablePrefix), new StringMapper());
    }
    
    public List<Execution> findExecutions(Map<String, Object> params, int limit) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        StringBuilder whereClause = new StringBuilder();
        int i = params.size();
        for (String columnName : params.keySet()) {
            whereClause.append(
                    StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(columnName), "_"))
                    .append(" = ? ");
            if (--i > 0) {
                whereClause.append("and ");
            }
        }
        return template.query(String.format(
                "select id, agent_id, flow_id, deployment_id, deployment_name, agent_name, "
                + "host_name, flow_name, status, start_time, end_time, create_by, last_update_by, parameters "
                        + "from %1$s_execution " + "where " + whereClause
                        + "order by create_time desc limit " + limit, tablePrefix),
                new ISqlRowMapper<Execution>() {
                    public Execution mapRow(Row row) {
                        Execution e = new Execution();
                        e.setId(row.getString("id"));
                        e.setAgentId(row.getString("agent_id"));
                        e.setFlowId(row.getString("flow_id"));
                        e.setAgentName(row.getString("agent_name"));
                        e.setHostName(row.getString("host_name"));
                        e.setFlowName(row.getString("flow_name"));
                        e.setDeploymentId(row.getString("deployment_id"));
                        e.setDeploymentName(row.getString("deployment_name"));
                        e.setStatus(row.getString("status"));
                        e.setStartTime(row.getDateTime("start_time"));
                        e.setEndTime(row.getDateTime("end_time"));
                        e.setCreateBy(row.getString("create_by"));
                        e.setLastUpdateBy(row.getString("last_update_by"));
                        e.setParameters(row.getString("parameters"));
                        return e;
                    }
                }, params.values().toArray());
    }
    
    @Override
    public void deleteExecution(String executionId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<String> executionStepIds = template.query(
                String.format("select id from %1$s_execution_step where execution_id = ?", tablePrefix),
                new StringMapper(),  executionId );
        for (String executionStepId : executionStepIds) {
            File file = new File(LogUtils.getLogDir(), executionStepId + ".log");
            FileUtils.deleteQuietly(file);
        }
        template.update(String.format(
                "delete from %1$s_execution_step where execution_id in (select id from %1$s_execution where id=?)", tablePrefix),
                executionId);
        template.update(String.format("delete from %1$s_execution where id=?", tablePrefix), executionId);
        log.info("Deleted execution with an id of {}", executionId);
    }

    @Override
    protected void purgeExecutions(String status, int retentionTimeInMs) {
        Table table = databasePlatform
                .readTableFromDatabase(null, null, tableName(Execution.class));
        if (table != null) {
            Date purgeBefore = DateUtils.addMilliseconds(new Date(), -retentionTimeInMs);
            log.debug("Purging executions with the status of {} before {}", status, purgeBefore);
            ISqlTemplate template = databasePlatform.getSqlTemplate();
            
            List<String> executionStepIds = template.query(
                    String.format("select id from %1$s_execution_step where execution_id in "
                            + "(select id from %1$s_execution where status=? and last_update_time <= ?)", tablePrefix),
                    new StringMapper(), new Object[] { status, purgeBefore });
            for (String executionStepId : executionStepIds) {
                File file = new File(LogUtils.getLogDir(), executionStepId + ".log");
                FileUtils.deleteQuietly(file);
            }
            
            int count = template
                    .update(String
                            .format("delete from %1$s_execution_step where execution_id in "
                                    + "(select id from %1$s_execution where status=? and last_update_time <= ?)",
                                    tablePrefix), status, purgeBefore);
            count += template.update(String.format(
                    "delete from %1$s_execution where status=? and last_update_time <= ?",
                    tablePrefix), status, purgeBefore);
            log.debug("Purged {} execution records with the status of {}", new Object[] { count, status });                
            if (!log.isDebugEnabled() && count > 0) {
                log.info("Purged {} execution records", new Object[] { count });                
            }
        } else {
            log.info("Could not run execution purge for status '{}' because table had not been created yet", status);
        }
    }

}
