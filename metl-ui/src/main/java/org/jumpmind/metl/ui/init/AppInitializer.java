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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.jumpmind.metl.core.model.GlobalSetting.CONFIG_BACKUP_CRON;
import static org.jumpmind.metl.core.model.GlobalSetting.CONFIG_BACKUP_ENABLED;
import static org.jumpmind.metl.core.model.GlobalSetting.DEFAULT_CONFIG_BACKUP_CRON;
import static org.jumpmind.metl.core.model.GlobalSetting.DEFAULT_CONFIG_BACKUP_ENABLED;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.atmosphere.container.JSR356AsyncSupport;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.job.BackupJob;
import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.AuditEvent.EventType;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.model.Version;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.util.AppConstants;
import org.jumpmind.metl.core.util.DatabaseScriptContainer;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.metl.ui.persist.IUICache;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

public class AppInitializer implements WebApplicationInitializer, ServletContextListener {

    public static ThreadLocal<AnnotationConfigWebApplicationContext> applicationContextRef = new ThreadLocal<>();
    
    ThreadPoolTaskScheduler jobScheduler;
    
    Properties properties;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        System.out.println("Version: " + VersionUtils.getCurrentVersion());
        properties = loadProperties();
        LogUtils.initLogging(AppUtils.getBaseDir(), (TypedProperties) properties);
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.scan("org.jumpmind.metl");
        MutablePropertySources sources = applicationContext.getEnvironment().getPropertySources();
        sources.addLast(new PropertiesPropertySource("passed in properties", properties));
        servletContext.addListener(new ContextLoaderListener(applicationContext));
        servletContext.addListener(this);
        servletContext.addListener(new RequestContextListener());

        AnnotationConfigWebApplicationContext dispatchContext = new AnnotationConfigWebApplicationContext();
        dispatchContext.setParent(applicationContext);
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("dispatcher", new DispatcherServlet(dispatchContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/api/*");
        applicationContextRef.set(dispatchContext);

        ServletRegistration.Dynamic apidocs = servletContext.addServlet("docs", DefaultServlet.class);
        apidocs.addMapping("/api.html", "/ws-api.html", "/doc/*", "/ace/*");
        
        Class appServletClazz = AppServlet.class;
        String appServletString = System.getProperty("metl.app.servlet");
        try {
            if (appServletString != null) {
                appServletClazz = Class.forName(appServletString);
                applicationContext.scan("com.jumpmind.metl");
            }
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Unable to load app servlet " + appServletString);
        }
        ServletRegistration.Dynamic vaadin = servletContext.addServlet("vaadin", appServletClazz);
        vaadin.setAsyncSupported(true);
        vaadin.setInitParameter("org.atmosphere.cpr.asyncSupport", JSR356AsyncSupport.class.getName());
        vaadin.setInitParameter("beanName", "appUI");
        vaadin.addMapping("/*");
    }

    protected void initPlugins(WebApplicationContext ctx) {
        InputStream is = ctx.getServletContext().getResourceAsStream("/plugins.zip");
        boolean newPluginsUnzipped = false;
        if (is != null) {
            ZipInputStream zip = null;
            try {
                zip = new ZipInputStream(is);
                ZipEntry entry = null;
                File dir = new File(AppUtils.getPluginsDir());                
                for (entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    File f = new File(dir, entry.getName());
                    if (!f.exists() || (f.getName().startsWith("maven-metadata") && newPluginsUnzipped)) {
                        if (entry.isDirectory()) {
                            f.mkdirs();
                        } else {
                            getLogger().info("Extracting: " + f.getAbsolutePath());
                            newPluginsUnzipped = true;
                            f.getParentFile().mkdirs();
                            OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                            try {
                                final byte buffer[] = new byte[4096];
                                int readCount;
                                while ((readCount = zip.read(buffer)) > 0) {
                                    os.write(buffer, 0, readCount);
                                }
                            } finally {
                                os.close();
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(zip);
            }
        }
        
        ctx.getBean(IPluginManager.class).init();
        ctx.getBean(IDefinitionFactory.class).refresh();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());        
        cleanTempJettyDirectories();
        initDatabase(ctx);
        initPlugins(ctx);        
        auditStartup(ctx);
        initUICache(ctx);
        initAgentRuntime(ctx);
        initBackgroundJobs(ctx);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (jobScheduler != null) {
            jobScheduler.destroy();
        }        
    }
    
    protected void initUICache(WebApplicationContext ctx) {
        ctx.getBean(IUICache.class).init();
    }
    
    protected void initBackgroundJobs(WebApplicationContext ctx) {
        try {
            IConfigurationService configurationService = ctx.getBean(IConfigurationService.class);
            IImportExportService importExportService = ctx.getBean(IImportExportService.class);
            IOperationsService operationsService = ctx.getBean(IOperationsService.class);
            
            jobScheduler = new ThreadPoolTaskScheduler();
            jobScheduler.setDaemon(true);
            jobScheduler.setThreadNamePrefix("background-job-");
            jobScheduler.setPoolSize(2);
            jobScheduler.initialize();

            TypedProperties properties = operationsService.findGlobalSetttingsAsProperties();
            if (properties.is(CONFIG_BACKUP_ENABLED, DEFAULT_CONFIG_BACKUP_ENABLED)) {
                jobScheduler.schedule(new BackupJob(importExportService, configurationService, operationsService, AppUtils.getBaseDir()),
                        new CronTrigger(
                                properties.get(CONFIG_BACKUP_CRON, DEFAULT_CONFIG_BACKUP_CRON)));
            }
            jobScheduler.scheduleAtFixedRate(() -> configurationService.doInBackground(), 600000);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).info("Failed to schedule the backup job", e);
        }
    }
    
    protected void auditStartup(WebApplicationContext ctx) {
        IConfigurationService service = ctx.getBean(IConfigurationService.class);
        service.save(new AuditEvent(EventType.RESTART, "Server starting...", AppConstants.SYSTEM_USER));
    }
    
    protected void cleanTempJettyDirectories() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (tempDir.exists() && tempDir.isDirectory()) {
            File[] files = tempDir.listFiles();
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith("jetty") && file.getName().contains("metl") && file.lastModified() < (System.currentTimeMillis()-24*60*60*1000)) {
                    try {
                        LoggerFactory.getLogger(getClass()).info("Purging " + file.getAbsolutePath());
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                    }                    
                }
            }
        }
    }

    protected void initAgentRuntime(WebApplicationContext ctx) {
        IAgentManager agentManger = ctx.getBean(IAgentManager.class);
        Thread startupThread = new Thread(()->agentManger.start());
        startupThread.setName("agent-manager-startup");
        startupThread.start();
    }

    protected void initDatabase(WebApplicationContext ctx) {
        if (isBlank(System.getProperty("h2.baseDir"))) {
            System.setProperty("h2.baseDir", AppUtils.getDatabaseDir());
            System.out.println("Setting H2 directory: " + AppUtils.getDatabaseDir());
        }

        IDatabasePlatform platform = ctx.getBean("configDatabasePlatform", IDatabasePlatform.class);
        IConfigurationService configurationService = ctx.getBean(IConfigurationService.class);
        boolean isInstalled = configurationService.isInstalled();
        DatabaseScriptContainer dbUpgradeScripts = new DatabaseScriptContainer("/org/jumpmind/metl/core/upgrade", platform);        
        String fromVersion = configurationService.getLastKnownVersion();
        String toVersion = VersionUtils.getCurrentVersion();
        if (fromVersion != null && !fromVersion.equals(toVersion)) {
            dbUpgradeScripts.executePreInstallScripts(fromVersion, toVersion);
        }
        ctx.getBean("configDatabaseUpgrader", ConfigDatabaseUpgrader.class).upgrade();
        platform.resetCachedTableModel();
        ctx.getBean("executionDatabaseUpgrader", ConfigDatabaseUpgrader.class).upgrade();
        
        if (fromVersion != null && !fromVersion.equals(toVersion)) {
            dbUpgradeScripts.executePostInstallScripts(fromVersion, toVersion);
        }

        if (fromVersion == null || !fromVersion.equals(toVersion)) {
            configurationService.save(new Version(toVersion));
        }
        if (!isInstalled) {
            try {
                IImportExportService importExportService = ctx.getBean(IImportExportService.class);
                LoggerFactory.getLogger(getClass()).info("Installing Metl samples");
                importExportService.importConfiguration(IOUtils.toString(getClass().getResourceAsStream("/metl-samples.json")), AppConstants.SYSTEM_USER);
            } catch (Exception e) {
                getLogger().error("Failed to install Metl samples", e);
            }
            
            configurationService.save(new PluginRepository("default", "http://maven.jumpmind.com/repo"));
            configurationService.save(new PluginRepository("central", "http://repo1.maven.org/maven2"));
        }
        getLogger().info("The configuration database has been initialized");
    }

    protected Properties loadProperties() {
        Properties properties = new Properties();
        String configDir = AppUtils.getConfigDir();
        File configFile = new File(configDir, "metl.properties");
        if (configFile.exists()) {
            properties = new TypedProperties(configFile);
        } else {
            try {
                System.out.println(
                        "Could not find the " + configFile.getAbsolutePath() + " configuration file.  A default version will be written.");
                String propContent = IOUtils.toString(getClass().getResourceAsStream("/" + configFile.getName()));
                propContent = FormatUtils.replaceToken(propContent, "configDir", configDir, true);
                properties = new TypedProperties(new ByteArrayInputStream(propContent.getBytes()));
                properties.put("log.to.console.enabled", System.getProperty("log.to.console.enabled", "false"));
                FileWriter writer = new FileWriter(configFile);
                properties.store(writer , "Generated on " + new Date());
                IOUtils.closeQuietly(writer);                
            } catch (IOException e) {
                throw new IoException(e);
            }
        }
        return properties;
    }

}
