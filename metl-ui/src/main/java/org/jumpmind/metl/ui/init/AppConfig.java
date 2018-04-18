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
package org.jumpmind.metl.ui.init;

import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_CONNECTION_PROPERTIES;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_DRIVER;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_INITIAL_SIZE;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_INIT_SQL;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_MAX_ACTIVE;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_MAX_IDLE;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_MAX_WAIT;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_MIN_IDLE;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_PASSWORD;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_TEST_ON_BORROW;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_TEST_ON_RETURN;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_TEST_WHILE_IDLE;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_URL;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_USER;
import static org.jumpmind.db.util.BasicDataSourcePropertyConstants.DB_POOL_VALIDATION_QUERY;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.activemq.Service;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.dbcp.BasicDataSource;
import org.h2.Driver;
import org.h2.tools.Server;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlException;
import org.jumpmind.db.sql.SqlPersistenceManager;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.metl.core.persist.ExecutionService;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.persist.IPluginService;
import org.jumpmind.metl.core.persist.ImportExportService;
import org.jumpmind.metl.core.persist.OperationsService;
import org.jumpmind.metl.core.persist.PluginService;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.core.plugin.PluginManager;
import org.jumpmind.metl.core.runtime.AgentManager;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.component.ComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.subscribe.ISubscribeManager;
import org.jumpmind.metl.core.runtime.subscribe.SubscribeManager;
import org.jumpmind.metl.core.runtime.web.HttpRequestMappingRegistry;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.security.SecurityConstants;
import org.jumpmind.metl.core.security.SecurityService;
import org.jumpmind.metl.core.util.EnvConstants;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.util.MockJdbcDriver;
import org.jumpmind.metl.ui.definition.DefinitionPlusUIFactory;
import org.jumpmind.metl.ui.definition.IDefinitionPlusUIFactory;
import org.jumpmind.metl.ui.persist.AuditableConfigurationService;
import org.jumpmind.metl.ui.persist.IUICache;
import org.jumpmind.metl.ui.persist.UICache;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.security.SecurityServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.cybercom.vaadin.spring.UIScope;

import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableTransactionManagement
@EnableWebMvc
@EnableSwagger2
public class AppConfig extends WebMvcConfigurerAdapter {

    protected static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    protected static final String EXECUTION = "execution.";

    @Autowired
    Environment env;

    IDatabasePlatform configDatabasePlatform;

    IDatabasePlatform executionDatabasePlatform;

    IConfigurationService configurationService;

    IImportExportService importExportService;

    IComponentRuntimeFactory componentRuntimeFactory;

    IPersistenceManager persistenceManager;

    IPersistenceManager executionPersistenceManager;

    IExecutionService executionService;

    ISecurityService securityService;

    IHttpRequestMappingRegistry httpRequestMappingRegistry;

    IPluginManager pluginManager;

    IDefinitionPlusUIFactory componentDefinitionPlusUIFactory;
    
    ISubscribeManager subscribeManager;

    BasicDataSource configDataSource;

    BasicDataSource executionDataSource;

    Service brokerService;

    MockJdbcDriver mockDriver;

    Server h2Server;

    IPluginService pluginService;

    IOperationsService operationsService;
    
    IUICache uiCache;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON).favorParameter(true).mediaType("xml", MediaType.APPLICATION_XML);
    }

    @Bean
    public Docket swaggerSpringMvcPlugin() {
        return new Docket(DocumentationType.SWAGGER_2).produces(contentTypes()).consumes(contentTypes())
                .apiInfo(new ApiInfo("Metl API", "This is the REST API for Metl", null, null, (Contact) null, null, null));
    }

    protected Set<String> contentTypes() {
        Set<String> set = new HashSet<String>();
        set.add("application/xml");
        set.add("application/json");
        return set;
    }

    @Bean
    @Scope(value = "singleton")
    Server h2Server() {
        String configDbUrl = env.getProperty(DB_POOL_URL, "jdbc:h2:mem:config");
        String execDbUrl = env.getProperty(EXECUTION + DB_POOL_URL, configDbUrl);
        if (h2Server == null && (configDbUrl.contains("h2:tcp") || execDbUrl.contains("h2:tcp"))) {
            try {
                h2Server = Server.createTcpServer("-tcpPort", env.getProperty("h2.port", "9092"));
                h2Server.start();
            } catch (SQLException e) {
                throw new SqlException(e);
            }
        }
        return h2Server;
    }

    @Bean
    @Scope(value = "singleton")
    MockJdbcDriver mockDriver() {
        if (mockDriver == null) {
            mockDriver = new MockJdbcDriver(configurationService());
            try {
                DriverManager.registerDriver(mockDriver);
            } catch (SQLException e) {
                log.error("", e);
            }
        }
        return mockDriver;
    }
    
    @PostConstruct
    protected void  addShutdownHook() {        
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            log.info("The Metl shutdown hook is currently running");
            // IAgentManager agentManger = ctx.getBean(IAgentManager.class);
            // //agentManager.stop();
            // IExecutionService service = ctx.getBean(IExecutionService.class);
            // //service.stop();
            log.info("Shutting down and compacting the config database");
            new JdbcTemplate(configDataSource()).execute("SHUTDOWN COMPACT");
            log.info("Done shutting down and compacting the config database");
            try {
                configDataSource().close();
            } catch (SQLException e) {
            }
            log.info("Shutting down and compacting the execution database");
            new JdbcTemplate(executionDataSource()).execute("SHUTDOWN COMPACT");
            log.info("Done shutting down and compacting the execution database");
            try {
                executionDataSource().close();
            } catch (SQLException e) {
            }
            
            if (h2Server != null) {
                h2Server.stop();
            }
        }, "shutdown-thread"));
    }

    @Bean
    @Scope(value = "singleton")
    BasicDataSource configDataSource() {
        if (configDataSource == null) {
            h2Server();
            TypedProperties properties = new TypedProperties();
            properties.put(DB_POOL_DRIVER, env.getProperty(DB_POOL_DRIVER, Driver.class.getName()));
            properties.put(DB_POOL_URL, env.getProperty(DB_POOL_URL, "jdbc:h2:mem:config"));
            properties.put(DB_POOL_USER, env.getProperty(DB_POOL_USER));
            properties.put(DB_POOL_PASSWORD, env.getProperty(DB_POOL_PASSWORD));
            properties.put(DB_POOL_INITIAL_SIZE, env.getProperty(DB_POOL_INITIAL_SIZE, "20"));
            properties.put(DB_POOL_MAX_ACTIVE, env.getProperty(DB_POOL_MAX_ACTIVE, "20"));
            properties.put(DB_POOL_MAX_IDLE, env.getProperty(DB_POOL_MAX_IDLE, "20"));
            properties.put(DB_POOL_MIN_IDLE, env.getProperty(DB_POOL_MIN_IDLE, "20"));
            properties.put(DB_POOL_MAX_WAIT, env.getProperty(DB_POOL_MAX_WAIT, "30000"));
            properties.put(DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, env.getProperty(DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, "120000"));
            properties.put(DB_POOL_VALIDATION_QUERY, env.getProperty(DB_POOL_VALIDATION_QUERY));
            properties.put(DB_POOL_TEST_ON_BORROW, env.getProperty(DB_POOL_TEST_ON_BORROW, "false"));
            properties.put(DB_POOL_TEST_ON_RETURN, env.getProperty(DB_POOL_TEST_ON_RETURN, "false"));
            properties.put(DB_POOL_TEST_WHILE_IDLE, env.getProperty(DB_POOL_TEST_WHILE_IDLE, "true"));
            properties.put(DB_POOL_INIT_SQL, env.getProperty(DB_POOL_INIT_SQL));
            properties.put(DB_POOL_CONNECTION_PROPERTIES, env.getProperty(DB_POOL_CONNECTION_PROPERTIES));
            log.info(
                    "About to initialize the configuration datasource using the following driver:"
                            + " '{}' and the following url: '{}' and the following user: '{}'",
                    new Object[] { properties.get(DB_POOL_DRIVER), properties.get(DB_POOL_URL), properties.get(DB_POOL_USER) });

            configDataSource = BasicDataSourceFactory.create(properties, SecurityServiceFactory.create());
        }
        return configDataSource;
    }

    @Bean
    @Scope(value = "singleton")
    BasicDataSource executionDataSource() {
        if (executionDataSource == null) {
            TypedProperties properties = new TypedProperties();
            String executionUrl = env.getProperty(EXECUTION + DB_POOL_URL);
            String appendToProperties = EXECUTION;
            if (executionUrl == null) {
                appendToProperties = "";
                executionUrl = env.getProperty(DB_POOL_URL, "jdbc:h2:mem:exec");
            }            
            properties.put(DB_POOL_URL, executionUrl);
            properties.put(DB_POOL_DRIVER,
                    env.getProperty(appendToProperties + DB_POOL_DRIVER, env.getProperty(DB_POOL_DRIVER, Driver.class.getName())));
            properties.put(DB_POOL_USER, env.getProperty(appendToProperties + DB_POOL_USER, env.getProperty(DB_POOL_USER)));
            properties.put(DB_POOL_PASSWORD, env.getProperty(appendToProperties + DB_POOL_PASSWORD, env.getProperty(DB_POOL_PASSWORD)));
            properties.put(DB_POOL_INITIAL_SIZE, env.getProperty(appendToProperties + DB_POOL_INITIAL_SIZE, "20"));
            properties.put(DB_POOL_MAX_ACTIVE, env.getProperty(appendToProperties + DB_POOL_MAX_ACTIVE, "20"));
            properties.put(DB_POOL_MAX_IDLE, env.getProperty(appendToProperties + DB_POOL_MAX_IDLE, "20"));
            properties.put(DB_POOL_MIN_IDLE, env.getProperty(appendToProperties + DB_POOL_MIN_IDLE, "20"));
            properties.put(DB_POOL_MAX_WAIT, env.getProperty(appendToProperties + DB_POOL_MAX_WAIT, "30000"));
            properties.put(DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, env.getProperty( appendToProperties + DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, "120000"));
            properties.put(DB_POOL_VALIDATION_QUERY, env.getProperty(appendToProperties + DB_POOL_VALIDATION_QUERY));
            properties.put(DB_POOL_TEST_ON_BORROW, env.getProperty(appendToProperties + DB_POOL_TEST_ON_BORROW, "false"));
            properties.put(DB_POOL_TEST_ON_RETURN, env.getProperty(appendToProperties + DB_POOL_TEST_ON_RETURN, "false"));
            properties.put(DB_POOL_TEST_WHILE_IDLE, env.getProperty(appendToProperties + DB_POOL_TEST_WHILE_IDLE, "true"));
            properties.put(EXECUTION + DB_POOL_INIT_SQL, env.getProperty(appendToProperties + DB_POOL_INIT_SQL));
            properties.put(DB_POOL_CONNECTION_PROPERTIES, env.getProperty(appendToProperties + DB_POOL_CONNECTION_PROPERTIES));
            log.info(
                    "About to initialize the execution datasource using the following driver:"
                            + " '{}' and the following url: '{}' and the following user: '{}'",
                    new Object[] { properties.get(DB_POOL_DRIVER), executionUrl, properties.get(DB_POOL_USER) });

            executionDataSource = BasicDataSourceFactory.create(properties, SecurityServiceFactory.create());
        }
        return executionDataSource;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IDatabasePlatform configDatabasePlatform() {
        if (configDatabasePlatform == null) {
            configDatabasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(configDataSource(), new SqlTemplateSettings(),
                    false, false);
        }
        return configDatabasePlatform;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IDatabasePlatform executionDatabasePlatform() {
        if (executionDatabasePlatform == null) {
            executionDatabasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(executionDataSource(),
                    new SqlTemplateSettings(), false, false);
        }
        return executionDatabasePlatform;
    }

    @Bean
    @Scope(value = "singleton")
    public String tablePrefix() {
        String tablePrefix = env.getProperty(EnvConstants.TABLE_PREFIX, "METL");
        return configDatabasePlatform().alterCaseToMatchDatabaseDefaultCase(tablePrefix);
    }

    @Bean
    @Scope(value = "singleton")
    public String configDir() {
        return AppUtils.getConfigDir();
    }

    @Bean
    @Scope(value = "singleton")
    public boolean logToFileEnabled() {
        return Boolean.parseBoolean(env.getProperty(EnvConstants.LOG_TO_FILE_ENABLED, "true"));
    }

    @Bean
    @Scope(value = "singleton")
    public String logFile() {
        return env.getProperty(EnvConstants.LOG_FILE, LogUtils.getLogFilePath());
    }

    @Bean
    @Scope(value = "singleton")
    public ConfigDatabaseUpgrader configDatabaseUpgrader() {
        return new ConfigDatabaseUpgrader("/schema.xml", configDatabasePlatform(), true, tablePrefix());
    }

    @Bean
    @Scope(value = "singleton")
    public ConfigDatabaseUpgrader executionDatabaseUpgrader() {
        return new ConfigDatabaseUpgrader("/schema-exec.xml", executionDatabasePlatform(), true, tablePrefix());
    }

    @Bean
    @Scope(value = "singleton")
    public IPersistenceManager persistenceManager() {
        if (persistenceManager == null) {
            persistenceManager = new SqlPersistenceManager(configDatabasePlatform());
        }
        return persistenceManager;
    }

    @Bean
    @Scope(value = "singleton")
    public IPersistenceManager executionPersistenceManager() {
        if (executionPersistenceManager == null) {
            executionPersistenceManager = new SqlPersistenceManager(executionDatabasePlatform());
        }
        return executionPersistenceManager;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IOperationsService operationsService() {
        if (operationsService == null) {
            operationsService = new OperationsService(securityService(), persistenceManager(), configDatabasePlatform(), tablePrefix());
        }
        return operationsService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IConfigurationService configurationService() {
        if (configurationService == null) {
            configurationService = new AuditableConfigurationService(operationsService(), securityService(), configDatabasePlatform(),
                    persistenceManager(), tablePrefix());
        }
        return configurationService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IPluginService pluginServive() {
        if (pluginService == null) {
            pluginService = new PluginService(securityService(), persistenceManager(), configDatabasePlatform(), tablePrefix());
        }
        return pluginService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IImportExportService importExportService() {
        if (importExportService == null) {
            importExportService = new ImportExportService(configDatabasePlatform(), persistenceManager(), tablePrefix(),
                    configurationService(), operationsService(), securityService());
        }
        return importExportService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IExecutionService executionService() {
        if (executionService == null) {
            executionService = new ExecutionService(securityService(), executionPersistenceManager(), executionDatabasePlatform(),
                    tablePrefix(), env);
        }
        return executionService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IPluginManager pluginManager() {
        if (pluginManager == null) {
            pluginManager = new PluginManager(AppUtils.getPluginsDir(), pluginServive());
        }
        return pluginManager;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IComponentRuntimeFactory componentRuntimeFactory() {
        if (componentRuntimeFactory == null) {
            componentRuntimeFactory = new ComponentRuntimeFactory(componentDefinitionPlusUIFactory());
        }
        return componentRuntimeFactory;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IDefinitionPlusUIFactory componentDefinitionPlusUIFactory() {
        if (componentDefinitionPlusUIFactory == null) {
            componentDefinitionPlusUIFactory = new DefinitionPlusUIFactory(pluginServive(), configurationService(), pluginManager());
        }
        return componentDefinitionPlusUIFactory;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IAgentManager agentManager() {
        IAgentManager agentManager = new AgentManager(operationsService(), configurationService(), executionService(), componentRuntimeFactory(),
                componentDefinitionPlusUIFactory(), httpRequestMappingRegistry(), subscribeManager());
        return agentManager;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IHttpRequestMappingRegistry httpRequestMappingRegistry() {
        if (httpRequestMappingRegistry == null) {
            httpRequestMappingRegistry = new HttpRequestMappingRegistry();
        }
        return httpRequestMappingRegistry;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public ISecurityService securityService() {
        if (securityService == null) {
            try {
                securityService = (ISecurityService) Class
                        .forName(System.getProperty(SecurityConstants.CLASS_NAME_SECURITY_SERVICE, SecurityService.class.getName()))
                        .newInstance();
                securityService.setConfigDir(AppUtils.getBaseDir());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return securityService;
    }
    
    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    IUICache uiCache() {
        if (uiCache == null) {
            uiCache = new UICache(importExportService(), configurationService());
        }
        return uiCache;
    }


    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    Service brokerService() {
        if (brokerService == null) {
            try {
                BrokerService broker = new BrokerService();
                broker.setPersistent(false);
                broker.getSystemUsage().getMemoryUsage().setLimit(10*1024*10);
                broker.start();
                brokerService = broker;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return brokerService;
    }
    
    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public ISubscribeManager subscribeManager() {
        if (subscribeManager == null) {
            subscribeManager = new SubscribeManager();
        }
        return subscribeManager;
    }

    @Bean
    static UIScope uiScope() {
        return new UIScope();
    }

}
