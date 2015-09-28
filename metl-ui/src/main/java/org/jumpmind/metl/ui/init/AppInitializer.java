package org.jumpmind.metl.ui.init;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.SqlScript;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.util.LogUtils;
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

    protected static final String SYS_CONFIG_DIR = AppUI.class.getPackage().getName() + ".config.dir";

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

        ServletRegistration.Dynamic vaadin = servletContext.addServlet("vaadin", new AppServlet());
        vaadin.addMapping("/VAADIN/*", "/app/*");
        vaadin.setInitParameter("beanName", "appUI");
        vaadin.setAsyncSupported(true);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
        LogUtils.initLogging(getConfigDir(false), ctx);
        initDatabase(ctx);
        initAgentRuntime(ctx);
    }

    protected void initAgentRuntime(WebApplicationContext ctx) {
        IAgentManager agentManger = ctx.getBean(IAgentManager.class);
        agentManger.start();
    }

    protected void initDatabase(WebApplicationContext ctx) {
        IDatabasePlatform platform = ctx.getBean(IDatabasePlatform.class);
        IConfigurationService configurationService = ctx.getBean(IConfigurationService.class);
        boolean isInstalled = configurationService.isInstalled();
        ConfigDatabaseUpgrader dbUpgrader = ctx.getBean(ConfigDatabaseUpgrader.class);
        dbUpgrader.upgrade();
        if (!isInstalled) {
            LoggerFactory.getLogger(getClass()).info("Installing Metl samples");
            new SqlScript(new InputStreamReader(getClass().getResourceAsStream("/metl-samples.sql")), platform.getSqlTemplate(), true, ";",
                    null).execute();
        }
        LoggerFactory.getLogger(getClass()).info("The configuration database has been initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    protected String getConfigDir(boolean printInstrutions) {
        String configDir = System.getProperty(SYS_CONFIG_DIR);
        if (isBlank(configDir)) {
            configDir = System.getProperty("user.home") + "/.metl";
            if (printInstrutions) {
                System.out.println("You can configure the following system property to point to a working directory "
                        + "where configuration files can be found: -D" + SYS_CONFIG_DIR + "=/some/config/dir");
            }
        }
        if (printInstrutions) {
            System.out.println("The current config directory is " + configDir);
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
                FileUtils.write(configFile, propContent);
                properties = new TypedProperties(configFile);
            } catch (IOException e) {
                throw new IoException(e);
            }
        }
        return properties;
    }

}
