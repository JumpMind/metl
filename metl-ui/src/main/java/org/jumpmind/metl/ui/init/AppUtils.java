package org.jumpmind.metl.ui.init;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.jumpmind.metl.core.util.AppConstants;

public class AppUtils {
    
    protected static String baseDir;

    protected static String configDir;

    protected static String databaseDir;

    protected static String pluginsDir;
    
    protected static String usersDir;

    static {
        if (StringUtils.isNotBlank(System.getProperty(AppConstants.SYS_CONFIG_DIR))) {
            baseDir = System.getProperty(AppConstants.SYS_CONFIG_DIR);
        } else {
            baseDir = System.getProperty("user.dir");
        }

        if (StringUtils.isNotBlank(System.getProperty(AppConstants.METL_APP_SERVLET))) {
            configDir = baseDir + File.separator + "conf";
            databaseDir = baseDir + File.separator + "db";
            usersDir = baseDir + File.separator + "users";
        } else {
            configDir = databaseDir = usersDir = baseDir;

            System.out.println();
            System.out.println("The current config directory is " + baseDir);
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
