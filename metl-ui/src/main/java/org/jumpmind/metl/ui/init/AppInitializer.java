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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

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
import org.jumpmind.metl.core.model.Version;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.util.DatabaseScriptContainer;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

public class AppInitializer implements WebApplicationInitializer, ServletContextListener {

    protected static final String SYS_CONFIG_DIR = "org.jumpmind.metl.ui.init.config.dir";

    public static ThreadLocal<AnnotationConfigWebApplicationContext> applicationContextRef = new ThreadLocal<>();

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        Properties properties = loadProperties();
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
        apidocs.addMapping("/api.html", "/doc/*", "/ace/*");
        
        ServletRegistration.Dynamic vaadin = servletContext.addServlet("vaadin", AppServlet.class);
        vaadin.setAsyncSupported(true);
        vaadin.setInitParameter("org.atmosphere.cpr.asyncSupport", JSR356AsyncSupport.class.getName());
        vaadin.setInitParameter("beanName", "appUI");
        vaadin.addMapping("/*");     
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
        LogUtils.initLogging(getConfigDir(false), ctx);
        cleanTempJettyDirectories();
        initDatabase(ctx);
        initAgentRuntime(ctx);
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
                importExportService.importConfiguration(IOUtils.toString(getClass().getResourceAsStream("/metl-samples.json")));
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Failed to install Metl samples", e);
            }
        }        
        LoggerFactory.getLogger(getClass()).info("The configuration database has been initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    protected String getConfigDir(boolean printInstructions) {
        String configDir = System.getProperty(SYS_CONFIG_DIR);
        if (isBlank(configDir)) {
            configDir = System.getProperty("user.home") + "/.metl";
            if (printInstructions) {
                System.out.println("You can configure the following system property to point to a working directory "
                        + "where configuration files can be found:\n  -D" + SYS_CONFIG_DIR + "=/some/config/dir");
            }
        }
        
        if (isBlank(System.getProperty("h2.baseDir"))) {
            System.setProperty("h2.baseDir", configDir);
        }
        
        if (printInstructions) {
            System.out.println("");
            System.out.println("The current config directory is " + configDir);
            System.out.println("");
            System.out.println("The current working directory is " + System.getProperty("user.dir"));
            System.out.println("");
        }
        return configDir;
    }

    protected Properties loadProperties() {
        Properties properties = new Properties();
        String configDir = getConfigDir(true);
        File configFile = new File(configDir, "metl.properties");
        if (configFile.exists()) {
            properties = new TypedProperties(configFile);
        } else {
            try {
                System.out.println("Could not find the " + configFile.getAbsolutePath() + " configuration file.  A default version will be written.");
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
        properties.put("config.dir", configDir);
        return properties;
    }

}
