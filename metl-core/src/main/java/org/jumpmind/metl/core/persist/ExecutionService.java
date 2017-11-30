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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.util.FormatUtils;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class ExecutionService extends AbstractService implements IExecutionService {

    ThreadPoolTaskScheduler purgeScheduler;

    Environment environment;
    
    protected IDatabasePlatform databasePlatform;

    public ExecutionService(ISecurityService securityService, IPersistenceManager persistenceManager, IDatabasePlatform databasePlatform, String tablePrefix, Environment env) {
        super(securityService, persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
        this.environment = env;
        this.purgeScheduler = new ThreadPoolTaskScheduler();
        this.purgeScheduler.setThreadNamePrefix("execution-purge-job-");
        this.purgeScheduler.setPoolSize(1);
        this.purgeScheduler.initialize();
        this.purgeScheduler.setDaemon(true);
        int periodInMs = Integer.parseInt(environment.getProperty("execution.purge.job.period.time.ms", Integer.toString(1000 * 60 * 60)));
        Date firstScheduledRunTime = DateUtils.addMilliseconds(new Date(), periodInMs);
        log.info("Scheduling the purge job to run every {}ms.  The first scheduled run time is at {}", periodInMs, firstScheduledRunTime);
        this.purgeScheduler.scheduleWithFixedDelay(new PurgeExecutionHandler(), firstScheduledRunTime, periodInMs);
    }

    public Execution findExecution(String id) {
        Execution e = new Execution();
        e.setId(id);
        persistenceManager.refresh(e, null, null, tableName(e.getClass()));
        return e;
    }

    public List<ExecutionStep> findExecutionSteps(String executionId) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("executionId", executionId);
        List<ExecutionStep> steps = persistenceManager.find(ExecutionStep.class, args, null, null, tableName(ExecutionStep.class));
        Collections.sort(steps, new Comparator<ExecutionStep>() {
            @Override
            public int compare(ExecutionStep o1, ExecutionStep o2) {
                int order = new Integer(o1.getApproximateOrder()).compareTo(new Integer(o2.getApproximateOrder()));
                if (order == 0) {
                    order = new Integer(o1.getThreadNumber()).compareTo(new Integer(o2.getThreadNumber()));
                }
                return order;
            }
        });
        return steps;
    }

    @Override
    public List<ExecutionStepLog> findExecutionStepLogs(Set<String> executionStepIds, int limit) {
        return findExecutionStepLogs(executionStepIds, limit, null);
    }
    
    @Override
    public File getExecutionStepLog(String executionStepId) {
        return new File(LogUtils.getLogDir(), executionStepId + ".log");
    }
    
    protected List<ExecutionStepLog> findExecutionStepLogs(Set<String> executionStepIds, int limit, Set<String> statuses) {
        List<ExecutionStepLog> executionStepLogs = new ArrayList<>();
        for (String executionStepId : executionStepIds) {
            File file = getExecutionStepLog(executionStepId);
            if (file.exists()) {
                CsvReader reader = null;
                try {
                    reader = new CsvReader(file.getAbsolutePath(),'"',Charset.forName("UTF-8"));
                    long id = 1;
                    while (reader.readRecord()) {
                        if (limit > 0) {
                            String[] values = reader.getValues();
                            if (values != null && values.length > 2 && isNotBlank(values[0]) && isNotBlank(values[1])
                                    && isNotBlank(values[2])) {
                                String level = values[0];
                                if (statuses == null || statuses.size() == 0
                                        || statuses.contains(level)) {
                                    ExecutionStepLog stepLog = new ExecutionStepLog();
                                    stepLog.setExecutionStepId(executionStepId);
                                    stepLog.setCreateTime(FormatUtils.parseDate(values[1],
                                            FormatUtils.TIMESTAMP_PATTERNS));
                                    stepLog.setLevel(level);
                                    stepLog.setLogText(values[2]);
                                    stepLog.setId(Long.toString(id++));
                                    executionStepLogs.add(stepLog);
                                }
                            }
                            limit--;
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    log.error("", e);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            }
        }

        Collections.sort(executionStepLogs);
        return executionStepLogs;
    }

    @Override
    public List<ExecutionStepLog> findExecutionStepLogs(String executionStepId, int limit) {
        Set<String> executionStepIds = new HashSet<>();
        executionStepIds.add(executionStepId);
        return findExecutionStepLogs(executionStepIds, limit);
    }
    
    @Override
    public List<ExecutionStepLog> findExecutionStepLogsInError(String executionStepId) {
        Set<String> executionStepIds = new HashSet<>();
        executionStepIds.add(executionStepId);
        Set<String> statuses = new HashSet<>();
        statuses.add(ExecutionStatus.ERROR.name());
        return findExecutionStepLogs(executionStepIds, Integer.MAX_VALUE, statuses);
    }

    class PurgeExecutionHandler implements Runnable {
        @Override
        public void run() {
            ExecutionStatus[] toPurge = new ExecutionStatus[] { ExecutionStatus.CANCELLED, ExecutionStatus.DONE, ExecutionStatus.ERROR,
                    ExecutionStatus.ABANDONED };
            for (ExecutionStatus executionStatus : toPurge) {
                String retentionTimeInMs = environment.getProperty("execution.retention.time.ms", Long.toString(1000 * 60 * 60 * 24 * 7));
                retentionTimeInMs = environment.getProperty("execution.retention.time.ms." + executionStatus.name().toLowerCase(),
                        retentionTimeInMs);
                purgeExecutions(executionStatus.name(), Integer.parseInt(retentionTimeInMs));
            }
        }
    }

    public void markAbandoned(String agentId) {
        log.info("Marking executions as abadoned for the agent with the id: {}", agentId);
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        int count = template.update(
                String.format(
                        "update %1$s_execution_step set status=? where execution_id in (select execution_id from %1$s_execution where agent_id=?) and (status=? or status=?) ",
                        tablePrefix), ExecutionStatus.ABANDONED.name(), agentId, ExecutionStatus.RUNNING
                        .name(), ExecutionStatus.READY.name());
        if (count > 0) {
            log.info("Updated {} execution step records that were abandoned", count);
        }
        count = template.update(
                String.format(
                        "update %1$s_execution set status=? where agent_id=? and (status=? or status=?)",
                        tablePrefix), ExecutionStatus.ABANDONED.name(), agentId, ExecutionStatus.RUNNING
                        .name(), ExecutionStatus.READY.name());
        if (count > 0) {
            log.info("Updated {} execution records that were abandoned", count);
        }
        
        log.info("Done marking executions as abandoned for agent with id: {}", agentId);
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
                        + "order by create_time desc", tablePrefix), limit,
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

    protected void purgeExecutions(String status, int retentionTimeInMs) {
        if (databasePlatform != null) {
            Table table = databasePlatform.readTableFromDatabase(null, null, tableName(Execution.class));
            if (table != null) {
                Date purgeBefore = DateUtils.addMilliseconds(new Date(), -retentionTimeInMs);
                log.debug("Purging executions with the status of {} before {}", status, purgeBefore);
                ISqlTemplate template = databasePlatform.getSqlTemplate();

                long ts = System.currentTimeMillis();
                
                List<String> executionIds = template.query(
                        String.format("select id from %1$s_execution where last_update_time <= ? and status=?", tablePrefix),
                        new StringMapper(), new Object[] { purgeBefore, status });
                
                if (0 > executionIds.size()) {
                   log.info("It took {}ms to find {} executions to purge with a status of {}", System.currentTimeMillis()-ts, executionIds.size(), status);
                }
                int countSteps = 0;
                int countExecutions = 0;
                for (String id : executionIds) {
                    List<String> executionStepIds = template.query(
                            String.format("select id from %1$s_execution_step where execution_id = ?", tablePrefix),
                            new StringMapper(), new Object[] { id });                    
                    for (String executionStepId : executionStepIds) {
                        File file = new File(LogUtils.getLogDir(), executionStepId + ".log");
                        FileUtils.deleteQuietly(file);
                        countSteps += template.update(String.format("delete from %1$s_execution_step where id=?", tablePrefix), executionStepId);
                    }
                    countExecutions += template.update(String.format("delete from %1$s_execution where id=?", tablePrefix), id);
                    
                    if (System.currentTimeMillis() - ts > 60000) {
                        log.info("Purged {} execution records and {} execution step records with the status of {} so far ...", new Object[] { countExecutions, countSteps, status });
                        ts = System.currentTimeMillis();
                    }
                }
                
                log.debug("Purged {} execution records and {} execution step records with the status of {}", new Object[] { countExecutions, countSteps, status });
                if (!log.isDebugEnabled() && (countSteps > 0 || countExecutions > 0)) {
                    log.info("Finished purging {} execution records and {} execution step records with the status of {}", new Object[] { countExecutions, countSteps, status });
                }
            } else {
                log.info("Could not run execution purge for status '{}' because table had not been created yet", status);
            }
        }
    }

}
