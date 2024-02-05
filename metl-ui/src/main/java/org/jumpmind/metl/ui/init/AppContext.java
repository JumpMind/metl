package org.jumpmind.metl.ui.init;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class AppContext extends AnnotationConfigWebApplicationContext {

    public AppContext() {
        super();
        System.out.println("Version: " + VersionUtils.getCurrentVersion());
        Properties properties = loadProperties();
        LogUtils.initLogging(AppUtils.getBaseDir(), (TypedProperties) properties);
        MutablePropertySources sources = getEnvironment().getPropertySources();
        sources.addLast(new PropertiesPropertySource("passed in properties", properties));
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
