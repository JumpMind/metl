package org.jumpmind.metl.core.runtime.flow;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.SqlPersistenceManager;
import org.jumpmind.db.sql.SqlScript;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.persist.ConfigurationSqlService;
import org.jumpmind.metl.core.persist.ExecutionSqlService;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.component.ComponentRuntimeFromXMLFactory;
import org.jumpmind.metl.core.runtime.resource.ResourceFactory;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.utils.DbTestUtils;
import org.jumpmind.util.AppUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.env.StandardEnvironment;

@RunWith(Parameterized.class)
public class TestProjectTest {

    static IDatabasePlatform databasePlatform;
    static SqlPersistenceManager persistenceManager;
    static ConfigurationSqlService configurationService;
    static ExecutionSqlService executionService;
    static AgentRuntime agentRuntime;

    FlowName flow;

    public TestProjectTest(FlowName flow) {
        this.flow = flow;
    }

    @Test
    public void testFlow() throws Exception {
        AgentDeployment deployment = agentRuntime.deploy(configurationService.findFlow(flow.getId()), new HashMap<>());
        String executionId = agentRuntime.scheduleNow(deployment);
        Execution execution = executionService.findExecution(executionId);
        while (execution != null
                && !execution.isDone()) {
            AppUtils.sleep(50);
            execution = executionService.findExecution(executionId);
        }       
        
        if (execution.isNotSuccess()) {
            AppUtils.sleep(50);
            List<ExecutionStep> steps = executionService.findExecutionSteps(executionId);
            StringBuilder message = new StringBuilder();
            for (ExecutionStep executionStep : steps) {
                if (executionStep.getExecutionStatus() == ExecutionStatus.ERROR) {
                    List<ExecutionStepLog> logs = executionService.findExecutionStepLogs(executionStep.getId());
                    message.append("'").append(executionStep.getComponentName()).append("'").append(" failed.  ");
                    for (ExecutionStepLog executionStepLog : logs) {
                        if (executionStepLog.getLogLevel() == LogLevel.ERROR) {
                            message.append(executionStepLog.getLogText());
                        }
                    }
                }
            }
            fail(message.toString());
        }
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> getFlows() throws Exception {
        File logDir = new File("working/logs");
        logDir.delete();
        logDir.mkdirs();
        LogUtils.setLogDir(logDir);
        System.setProperty("log.file", "working/logs/metl.log");
        databasePlatform = DbTestUtils.createDatabasePlatform();
        new ConfigDatabaseUpgrader("/schema-v1.xml", databasePlatform, true, "METL").upgrade();
        persistenceManager = new SqlPersistenceManager(databasePlatform);
        configurationService = new ConfigurationSqlService(databasePlatform, persistenceManager, "METL");
        executionService = new ExecutionSqlService(databasePlatform, persistenceManager, "METL", new StandardEnvironment());
        agentRuntime = new AgentRuntime(new Agent("test", AppUtils.getHostName()), configurationService, executionService, new ComponentRuntimeFromXMLFactory(),
                new ResourceFactory());
        agentRuntime.start();
        SqlScript script = new SqlScript(TestProjectTest.class.getResource("/metl-tests-config.sql"), databasePlatform.getSqlTemplate());
        script.execute();
        List<FlowName> flows = configurationService
                .findFlowsInProject(configurationService.findProjects().get(0).getLatestProjectVersion().getId(), true);
        List<Object[]> params = new ArrayList<>();
        for (FlowName flowName : flows) {
            params.add(new Object[] { flowName });
        }
        return params;

    }

}
