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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.util.FormatUtils;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

abstract public class AbstractExecutionService extends AbstractService implements IExecutionService {

    ThreadPoolTaskScheduler purgeScheduler;

    Environment environment;

    public AbstractExecutionService(IPersistenceManager persistenceManager, String tablePrefix, Environment env) {
        super(persistenceManager, tablePrefix);
        this.environment = env;
        this.purgeScheduler = new ThreadPoolTaskScheduler();
        this.purgeScheduler.setThreadNamePrefix("execution-purge-job-");
        this.purgeScheduler.setPoolSize(1);
        this.purgeScheduler.initialize();
        this.purgeScheduler.setDaemon(true);
        this.purgeScheduler.scheduleWithFixedDelay(new PurgeExecutionHandler(), 60000 * 5);
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

    abstract protected void purgeExecutions(String status, int retentionTimeInMs);

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

}
