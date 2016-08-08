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

import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.h2.Driver;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlPersistenceManager;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.metl.core.persist.ConfigurationSqlService;
import org.jumpmind.metl.core.persist.ExecutionSqlService;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.persist.ImportExportService;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.core.plugin.PluginManager;
import org.jumpmind.metl.core.runtime.AgentManager;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.component.ComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.core.runtime.resource.ResourceFactory;
import org.jumpmind.metl.core.runtime.web.HttpRequestMappingRegistry;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
import org.jumpmind.metl.core.util.EnvConstants;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.ui.common.AppConstants;
import org.jumpmind.metl.ui.views.ComponentXmlDefinitionPlusUIFactory;
import org.jumpmind.metl.ui.views.IComponentDefinitionPlusUIFactory;
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
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.cybercom.vaadin.spring.UIScope;

import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableTransactionManagement
@EnableWebMvc
@EnableSwagger2
public class AppConfig extends WebMvcConfigurerAdapter {

    protected static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Autowired
    Environment env;

    IDatabasePlatform databasePlatform;

    IConfigurationService configurationService;

    IImportExportService importExportService;

    IComponentRuntimeFactory componentRuntimeFactory;

    IResourceFactory resourceFactory;

    IPersistenceManager persistenceManager;

    IExecutionService executionService;

    IHttpRequestMappingRegistry httpRequestMappingRegistry;

    IPluginManager pluginManager;

    IComponentDefinitionPlusUIFactory componentDefinitionPlusUIFactory;

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
        return new Docket(DocumentationType.SWAGGER_2).pathMapping("/api").produces(contentTypes()).consumes(contentTypes())
                .apiInfo(new ApiInfo("Metl API", "This is the REST API for Metl", null, null, null, null, null));
    }

    protected Set<String> contentTypes() {
        Set<String> set = new HashSet<String>();
        set.add("application/xml");
        set.add("application/json");
        return set;
    }

    @Bean
    @Scope(value = "singleton")
    DataSource configDataSource() {
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

        DataSource configDataSource = BasicDataSourceFactory.create(properties, SecurityServiceFactory.create());
        return configDataSource;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IDatabasePlatform configDatabasePlatform() {
        if (databasePlatform == null) {
            databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(configDataSource(), new SqlTemplateSettings(), true,
                    false);
        }
        return databasePlatform;
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
        return env.getProperty(EnvConstants.CONFIG_DIR);
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
    public IPersistenceManager persistenceManager() {
        if (persistenceManager == null) {
            persistenceManager = new SqlPersistenceManager(configDatabasePlatform());
        }
        return persistenceManager;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IConfigurationService configurationService() {
        if (configurationService == null) {
            configurationService = new ConfigurationSqlService(configDatabasePlatform(), persistenceManager(), tablePrefix());
        }
        return configurationService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IImportExportService importExportService() {
        if (importExportService == null) {
            importExportService = new ImportExportService(configDatabasePlatform(), persistenceManager(), tablePrefix(),
                    configurationService);
        }
        return importExportService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IExecutionService executionService() {
        if (executionService == null) {
            executionService = new ExecutionSqlService(configDatabasePlatform(), persistenceManager(), tablePrefix(), env);
        }
        return executionService;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IPluginManager pluginManager() {
        if (pluginManager == null) {
            String localPluginDir = String.format("%s/%s", env.getProperty(AppConstants.PROP_CONFIG_DIR), AppConstants.PLUGINS_DIR);
            pluginManager = new PluginManager(localPluginDir, configurationService());
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
    public IComponentDefinitionPlusUIFactory componentDefinitionPlusUIFactory() {
        if (componentDefinitionPlusUIFactory == null) {
            componentDefinitionPlusUIFactory = new ComponentXmlDefinitionPlusUIFactory(configurationService(), pluginManager());
        }
        return componentDefinitionPlusUIFactory;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IResourceFactory resourceFactory() {
        if (resourceFactory == null) {
            resourceFactory = new ResourceFactory();
        }
        return resourceFactory;
    }

    @Bean
    @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
    public IAgentManager agentManager() {
        IAgentManager agentManager = new AgentManager(configurationService(), executionService(), componentRuntimeFactory(),
                componentDefinitionPlusUIFactory(), resourceFactory(), httpRequestMappingRegistry());
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
    static UIScope uiScope() {
        return new UIScope();
    }

}
