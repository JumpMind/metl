package org.jumpmind.symmetric.is.ui.init;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.app.core.ConfigDatabaseUpgrader;
import org.jumpmind.symmetric.app.core.LogUtils;
import org.jumpmind.symmetric.is.ui.support.Broadcaster;
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

    protected static final String SYS_CONFIG_DIR = AppUI.class.getPackage().getName()
            + ".config.dir";

    public static ThreadLocal<AnnotationConfigWebApplicationContext> applicationContextRef = new ThreadLocal<>();

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        Properties properties = loadProperties();
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.scan("org.jumpmind.symmetric.is");
        MutablePropertySources sources = applicationContext.getEnvironment().getPropertySources();
        sources.addLast(new PropertiesPropertySource("passed in properties", properties));
        servletContext.addListener(new ContextLoaderListener(applicationContext));
        servletContext.addListener(this);
        servletContext.addListener(new RequestContextListener());

        AnnotationConfigWebApplicationContext dispatchContext = new AnnotationConfigWebApplicationContext();
        dispatchContext.setParent(applicationContext);
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("dispatcher",
                new DispatcherServlet(dispatchContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/api/*");
        applicationContextRef.set(dispatchContext);

        ServletRegistration.Dynamic vaadin = servletContext.addServlet("vaadin", new AppServlet());
        vaadin.setInitParameter("org.atmosphere.cpr.broadcasterCacheClass",
                "org.atmosphere.cache.UUIDBroadcasterCache");
        vaadin.addMapping("/VAADIN/*", "/app/*");
        vaadin.setInitParameter("beanName", "appUI");
        vaadin.setAsyncSupported(true);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce
                .getServletContext());
        initDatabase(ctx);
        LogUtils.initLogging(ctx);
        initAgentRuntime(ctx);
    }

    protected void initAgentRuntime(WebApplicationContext ctx) {
    }

    protected void initDatabase(WebApplicationContext ctx) {
        ConfigDatabaseUpgrader dbUpgrader = ctx.getBean(ConfigDatabaseUpgrader.class);
        dbUpgrader.upgrade();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce
                .getServletContext());
        if (ctx != null) {
            Broadcaster.destroy();
        } else {
            System.err
                    .println("The web application context must have not been created, because we cannot find it for cleanup");
        }
    }

    protected Properties loadProperties() {
        Properties properties = new Properties();
        String configDir = System.getProperty(SYS_CONFIG_DIR);
        if (isBlank(configDir)) {
            configDir = System.getProperty("user.home") + "/.symmetricis";
            System.out
                    .println("You can configure the following system property to point to a working directory "
                            + "where configuration files can be found: -D"
                            + SYS_CONFIG_DIR
                            + "=/some/config/dir");
        }
        System.out.println("The current config directory is " + configDir);

        File configFile = new File(configDir, "symmetricis.properties");
        if (configFile.exists()) {
            properties = new TypedProperties(configFile);
        } else {
            System.out.println("Could not find the " + configFile.getAbsolutePath()
                    + " configuration file.  Looking on the classpath for " + configFile.getName());

            InputStream is = getClass().getResourceAsStream("/" + configFile.getName());
            if (is != null) {
                properties = new TypedProperties(is);
            } else {
                System.err.println("Could not find any " + configFile.getName()
                        + ".  Using all of the system defaults.");
            }
        }
        return properties;
    }

}
