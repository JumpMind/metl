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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.metl.ui.diagram.Diagram;
import org.jumpmind.metl.ui.diagram.RunDiagram;
import org.jumpmind.metl.ui.persist.IUICache;
import org.jumpmind.metl.ui.views.admin.AdminView;
import org.jumpmind.metl.ui.views.design.DesignView;
import org.jumpmind.metl.ui.views.explore.ExploreDataSourceView;
import org.jumpmind.metl.ui.views.explore.ExploreDirectoryView;
import org.jumpmind.metl.ui.views.explore.ExploreServicesView;
import org.jumpmind.metl.ui.views.manage.ManageView;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSelectionColumn;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.AnnotationValidator;
import com.vaadin.base.devserver.startup.DevModeInitializer;
import com.vaadin.flow.server.startup.ErrorNavigationTargetInitializer;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;
import com.vaadin.flow.server.startup.RouteRegistryInitializer;
import com.vaadin.flow.server.startup.ServletContextListeners;
import com.vaadin.flow.server.startup.VaadinInitializerException;

import de.f0rce.ace.AceEditor;

public class AppInitializer implements ServletContextListener {

    public static ThreadLocal<AnnotationConfigWebApplicationContext> applicationContextRef = new ThreadLocal<>();
    
    ThreadPoolTaskScheduler jobScheduler;
    
    Properties properties;
    
    ServletContextListeners servletContextListeners = new ServletContextListeners();

    protected void initPlugins(WebApplicationContext ctx) {
        boolean newPluginsUnzipped = false;
        try (InputStream is = ctx.getServletContext().getResourceAsStream("/plugins.zip")) {
            if (is != null) {
                try (ZipInputStream zip = new ZipInputStream(is)) {
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
                                Files.copy(zip, f.toPath());
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        ctx.getBean(IPluginManager.class).init();
        ctx.getBean(IDefinitionFactory.class).refresh();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        boolean productionMode = false;
        String productionModeString = sce.getServletContext().getInitParameter(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE);
        if (StringUtils.isNotEmpty(productionModeString)) {
            productionMode = Boolean.valueOf(productionModeString);
        }
        if (!productionMode) {
            // Run initializers with relevant classes from the classpath
            runInitializers(sce);
        }
        // Finalize initialization
        servletContextListeners.contextInitialized(sce);
        
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
        servletContextListeners.contextDestroyed(sce);
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
            configurationService.save(new PluginRepository("central", "https://repo1.maven.org/maven2"));
        }
        getLogger().info("The configuration database has been initialized");
    }
    
    private static void runInitializers(ServletContextEvent sce) {
        runInitializer(new LookupServletContainerInitializer(), sce, LookupInitializer.class);
        // Anything implementing HasErrorParameter
        runInitializer(new ErrorNavigationTargetInitializer(), sce, RouteNotFoundError.class,
                InternalServerError.class);
        // @Route
        runInitializer(new RouteRegistryInitializer(), sce, AdminView.class, DesignView.class,
                ExploreDataSourceView.class, ExploreDirectoryView.class, ExploreServicesView.class, ManageView.class);
        // @NpmPackage, @JsModule, @CssImport, @JavaScript or @Theme
        Set<Class<?>> devModeInitClassSet = new HashSet<Class<?>>();
        devModeInitClassSet.addAll(Arrays.asList(AceEditor.class, AppUI.class, Diagram.class, RunDiagram.class, Button.class,
                Checkbox.class, ComboBox.class, ContextMenu.class, DatePicker.class, Dialog.class, DragSource.class,
                DropTarget.class, FormItem.class, FormLayout.class, Grid.class, GridSelectionColumn.class,
                HorizontalLayout.class, Icon.class, ListBox.class, MenuBar.class, Notification.class, PasswordField.class,
                ProgressBar.class, RadioButtonGroup.class, Scroller.class, Select.class, SplitLayout.class, Tabs.class,
                TextArea.class, TextField.class, TreeGrid.class, Upload.class, VerticalLayout.class));
        try {
            DevModeInitializer.initDevModeHandler(devModeInitClassSet, VaadinService.getCurrent().getContext());
        } catch (VaadinInitializerException e) {
            throw new RuntimeException(e);
        }
        runInitializer(new AnnotationValidator(), sce, AdminView.class, DesignView.class,
                ExploreDataSourceView.class, ExploreDirectoryView.class, ExploreServicesView.class, ManageView.class);
    }

    private static void runInitializer(ServletContainerInitializer initializer, ServletContextEvent sce,
            Class<?>... types) {
        try {
            initializer.onStartup(new HashSet<>(Arrays.asList(types)), sce.getServletContext());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
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
                configFile.getParentFile().mkdirs();
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
