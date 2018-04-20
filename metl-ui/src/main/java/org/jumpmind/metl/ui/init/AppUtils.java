package org.jumpmind.metl.ui.init;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

public class AppUtils {

    protected static final String SYS_CONFIG_DIR = "org.jumpmind.metl.ui.init.config.dir";
    
    protected static String baseDir;

    protected static String configDir;

    protected static String databaseDir;

    protected static String pluginsDir;
    
    protected static String usersDir;

    static {
        if (StringUtils.isNotBlank(System.getProperty(SYS_CONFIG_DIR))) {
            baseDir = System.getProperty(SYS_CONFIG_DIR);
            configDir = databaseDir = usersDir = baseDir;

            System.out.println();
            System.out.println("The current config directory is " + baseDir);
        } else {
            baseDir = System.getProperty("user.dir");            
            configDir = baseDir + File.separator + "conf";
            databaseDir = baseDir + File.separator + "db";
            usersDir = baseDir + File.separator + "users";
        }

        pluginsDir = baseDir + File.separator + "plugins";
    }

    private AppUtils() {
    }

    public static String getBaseDir() {
        return baseDir;
    }

    public static String getConfigDir() {
        return configDir;
    }

    public static String getDatabaseDir() {
        return databaseDir;
    }

    public static String getPluginsDir() {
        return pluginsDir;
    }

}
