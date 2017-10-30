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
package org.jumpmind.metl.core.runtime;

import java.io.File;
import java.net.URL;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlPersistenceManager;
import org.jumpmind.db.sql.SqlScript;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.persist.ConfigurationService;
import org.jumpmind.metl.core.persist.ExecutionService;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.persist.ImportExportService;
import org.jumpmind.metl.core.persist.OperationsService;
import org.jumpmind.metl.core.persist.PluginService;
import org.jumpmind.metl.core.plugin.DefinitionFactory;
import org.jumpmind.metl.core.plugin.PluginManager;
import org.jumpmind.metl.core.runtime.component.ComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.flow.FlowRuntime;
import org.jumpmind.metl.core.runtime.subscribe.SubscribeManager;
import org.jumpmind.metl.core.runtime.web.HttpRequestMappingRegistry;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.security.SecurityService;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.util.MockJdbcDriver;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.properties.TypedProperties;
import org.springframework.core.env.StandardEnvironment;

public class StandaloneFlowRunner {

    File logDir;

    String configSqlScript;

    IDatabasePlatform databasePlatform;

    IPersistenceManager persistenceManager;

    IConfigurationService configurationService;

    IExecutionService executionService;
    
    IOperationsService operationsService;

    AgentRuntime agentRuntime;

    boolean includeTestFlows = true;

    boolean includeRegularFlows = true;

    public StandaloneFlowRunner() {
    }

    public void setConfigSqlScript(String configSqlScript) {
        this.configSqlScript = configSqlScript;
    }

    public void setLogDir(String logDir) {
        System.setProperty("log.file", logDir + "/metl.log");
        this.logDir = new File(logDir);
    }

    public void setIncludeRegularFlows(boolean includeRegularFlows) {
        this.includeRegularFlows = includeRegularFlows;
    }

    public void setIncludeTestFlows(boolean includeTestFlows) {
        this.includeTestFlows = includeTestFlows;
    }

    public List<FlowName> getFlows() {
        init();
        List<FlowName> flows = new ArrayList<>();
        List<Project> projects = configurationService.findProjects();
        for (Project project : projects) {
            String projectVersionId = project.getMasterVersion().getId();
            if (includeRegularFlows) {
                flows.addAll(configurationService.findFlowsInProject(projectVersionId, false));
            }
            if (includeTestFlows) {
                flows.addAll(configurationService.findFlowsInProject(projectVersionId, true));
            }
        }
        return flows;
    }

    public Execution findExecution(String executionId) {
        return executionService.findExecution(executionId);
    }

    public String runFlow(FlowName flow, boolean waitFor) throws Exception {
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        try {
            broker.start();
            AgentDeploy deployment = agentRuntime.deploy(configurationService.findFlow(flow.getId()), new HashMap<>());
            FlowRuntime runtime = agentRuntime.createFlowRuntime("standalone", deployment, new HashMap<>());
            runtime.execute();
            return runtime.getExecutionId();
        } finally {
            broker.stop();
        }

    }

    public String getFailureMessage(Execution execution) {
        List<ExecutionStep> steps = executionService.findExecutionSteps(execution.getId());
        StringBuilder message = new StringBuilder("The flow failed with a status of ").append(execution.getStatus()).append(".  ");
        for (ExecutionStep executionStep : steps) {
            if (executionStep.getExecutionStatus() == ExecutionStatus.ERROR) {
                List<ExecutionStepLog> logs = executionService.findExecutionStepLogsInError(executionStep.getId());
                message.append("'").append(executionStep.getComponentName()).append("'").append(" failed.  ");
                for (ExecutionStepLog executionStepLog : logs) {
                    message.append(executionStepLog.getLogText());
                }
            }
        }
        return message.toString();
    }

    protected void init() {
        if (agentRuntime == null) {
            try {
                logDir.delete();
                logDir.mkdirs();
                LogUtils.setLogDir(logDir);
                databasePlatform = initDatabasePlatform();
                new ConfigDatabaseUpgrader("/schema.xml", databasePlatform, true, "METL").upgrade();
                new ConfigDatabaseUpgrader("/schema-exec.xml", databasePlatform, true, "METL").upgrade();
                persistenceManager = new SqlPersistenceManager(databasePlatform);
                ISecurityService securityService = new SecurityService();
                final String tablePrefix = "METL";
                operationsService = new OperationsService(securityService, persistenceManager, databasePlatform, tablePrefix);
                configurationService = new ConfigurationService(operationsService, securityService, databasePlatform, persistenceManager, tablePrefix);
                MockJdbcDriver mockDriver = new MockJdbcDriver(configurationService);
                DriverManager.registerDriver(mockDriver);
                PluginService pluginService = new PluginService(securityService, persistenceManager, databasePlatform, tablePrefix);
                PluginManager pluginManager = new PluginManager("working/plugins", pluginService);
                pluginManager.init();

                DefinitionFactory componentDefinitionFactory = new DefinitionFactory(pluginService, configurationService, pluginManager);

                IImportExportService importService = new ImportExportService(databasePlatform, persistenceManager, tablePrefix,
                        configurationService, operationsService, new SecurityService());

                executionService = new ExecutionService(securityService, persistenceManager, databasePlatform, tablePrefix, new StandardEnvironment());
                agentRuntime = new AgentRuntime(new Agent("test"), operationsService, configurationService, executionService,
                        new ComponentRuntimeFactory(componentDefinitionFactory), componentDefinitionFactory,
                        new HttpRequestMappingRegistry(), new SubscribeManager());
                agentRuntime.start();
                URL configSqlScriptURL = null;
                File configSqlScriptFile = new File(configSqlScript);

                if (configSqlScriptFile.exists()) {
                    configSqlScriptURL = configSqlScriptFile.toURI().toURL();
                } else {
                    configSqlScriptURL = getClass().getResource(configSqlScript);
                }

                if (configSqlScript.toLowerCase().endsWith(".sql")) {
                    SqlScript script = new SqlScript(configSqlScriptURL, databasePlatform.getSqlTemplate());
                    script.execute();
                } else {
                    importService.importConfiguration(IOUtils.toString(configSqlScriptURL), "standalone");
                }

                componentDefinitionFactory.refresh();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected IDatabasePlatform initDatabasePlatform() {
        TypedProperties properties = new TypedProperties();
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_DRIVER, "org.h2.Driver");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_URL,
                "jdbc:h2:mem:" + FilenameUtils.removeExtension(configSqlScript).replaceAll("-", ""));
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_USER, "jumpmind");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_PASSWORD, "jumpmind");
        DataSource ds = BasicDataSourceFactory.create(properties);
        return JdbcDatabasePlatformFactory.createNewPlatformInstance(ds, new SqlTemplateSettings(), false, false);
    }

}
