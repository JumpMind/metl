package org.jumpmind.symmetric.is.ui.init;

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
import static org.jumpmind.symmetric.is.core.util.EnvConstants.DEFAULT_PROPERTY_ENV;
import static org.jumpmind.symmetric.is.core.util.EnvConstants.PROPERTY_ENV;

import javax.sql.DataSource;

import org.h2.Driver;
import org.jumpmind.db.persist.JdbcPersistenceManager;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.security.SecurityServiceFactory;
import org.jumpmind.symmetric.is.core.persist.ConfigurationSqlService;
import org.jumpmind.symmetric.is.core.persist.ExecutionSqlService;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;
import org.jumpmind.symmetric.is.core.runtime.AgentManager;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.util.EnvConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.cybercom.vaadin.spring.UIScope;

@Configuration
@EnableTransactionManagement
@EnableWebMvc
public class AppConfig {

    protected static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Autowired
    Environment env;

    IDatabasePlatform databasePlatform;

    IConfigurationService configurationService;

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    IPersistenceManager persistenceManager;
    
    IExecutionService executionService;

    @Bean
    @Scope(value = "singleton")
    DataSource configDataSource() {
        log.info("The current working directory is " + System.getProperty("user.dir"));
        log.info("The current environment is configured as "
                + env.getProperty(PROPERTY_ENV, DEFAULT_PROPERTY_ENV)
                + ".  This value can be changed in the application properties file using the "
                + PROPERTY_ENV + " property");
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
        properties.put(DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                env.getProperty(DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, "120000"));
        properties.put(DB_POOL_VALIDATION_QUERY, env.getProperty(DB_POOL_VALIDATION_QUERY));
        properties.put(DB_POOL_TEST_ON_BORROW, env.getProperty(DB_POOL_TEST_ON_BORROW, "false"));
        properties.put(DB_POOL_TEST_ON_RETURN, env.getProperty(DB_POOL_TEST_ON_RETURN, "false"));
        properties.put(DB_POOL_TEST_WHILE_IDLE, env.getProperty(DB_POOL_TEST_WHILE_IDLE, "true"));
        properties.put(DB_POOL_INIT_SQL, env.getProperty(DB_POOL_INIT_SQL));
        properties.put(DB_POOL_CONNECTION_PROPERTIES,
                env.getProperty(DB_POOL_CONNECTION_PROPERTIES));
        log.info("About to initialize the configuration datasource using the following driver:"
                + " '{}' and the following url: '{}' and the following user: '{}'",
                new Object[] { properties.get(DB_POOL_DRIVER), properties.get(DB_POOL_URL),
                        properties.get(DB_POOL_USER) });

        DataSource configDataSource = BasicDataSourceFactory.create(properties,
                SecurityServiceFactory.create());
        return configDataSource;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IDatabasePlatform configDatabasePlatform() {
        if (databasePlatform == null) {
            databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(
                    configDataSource(), new SqlTemplateSettings(), true);
        }
        return databasePlatform;
    }

    @Bean
    @Scope(value = "singleton")
    public String tablePrefix() {
        String tablePrefix = env.getProperty(EnvConstants.TABLE_PREFIX, "SIS");
        return configDatabasePlatform().alterCaseToMatchDatabaseDefaultCase(tablePrefix);
    }

    @Bean
    @Scope(value = "singleton")
    public ConfigDatabaseUpgrader configDatabaseUpgrader() {
        return new ConfigDatabaseUpgrader("/schema-v1.xml", configDatabasePlatform(), true,
                tablePrefix());
    }

    @Bean
    @Scope(value = "singleton")
    public IPersistenceManager persistenceManager() {
        if (persistenceManager == null) {
            persistenceManager = new JdbcPersistenceManager(configDatabasePlatform());
        }
        return persistenceManager;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IConfigurationService configurationService() {
        if (configurationService == null) {
            configurationService = new ConfigurationSqlService(configDatabasePlatform(),
                    persistenceManager(), tablePrefix());
        }
        return configurationService;
    }
    
    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IExecutionService executionService() {
        if (executionService == null) {
            executionService = new ExecutionSqlService(configDatabasePlatform(),
                    persistenceManager(), tablePrefix());
        }
        return executionService;
    }    

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IComponentFactory componentFactory() {
        if (componentFactory == null) {
            componentFactory = new ComponentFactory();
        }
        return componentFactory;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IConnectionFactory connectionFactory() {
        if (connectionFactory == null) {
            connectionFactory = new ConnectionFactory();
        }
        return connectionFactory;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IAgentManager agentManager() {
        IAgentManager agentManager = new AgentManager(configurationService(), componentFactory(),
                connectionFactory());
        return agentManager;
    }

    @Bean
    static UIScope uiScope() {
        return new UIScope();
    }
}
