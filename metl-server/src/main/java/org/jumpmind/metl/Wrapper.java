package org.jumpmind.metl;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.symmetric.wrapper.WrapperHelper;

public class Wrapper {

    protected static final String SYS_CONFIG_DIR = "org.jumpmind.metl.ui.init.config.dir";
    protected static final String METL_HOME = "METL_HOME";
    
    public static void runServiceWrapper(String[] args) throws Exception {

    	String configFileName="metl_service.conf";    	
    	String jarFileName="metl.war";
    	
        String configDir = getConfigDir(false);
    	String applHomeDir = new String(configDir);
    	
        File configFile = new File(configDir, configFileName);
        if (!configFile.exists()) {   
            try {
                String propContent = IOUtils.toString(Wrapper.class.getClassLoader().getResourceAsStream(configFileName));
                FileUtils.write(configFile, propContent);
            } catch (Exception e) {
            	System.out.println("Unable to write config file for service wrapper." + e.getMessage());
            	System.exit(-1);
            }
        }
        System.out.println("Calling WrapperHelper with parameters:  applHomeDir==>" + applHomeDir + ", configFile ==>" + 
        		configDir + "/" + configFileName + " jarfile==> " + 
        		configDir + File.separator + jarFileName);
        WrapperHelper.run(args, applHomeDir, configDir + File.separator + configFileName,
        		configDir + File.separator + jarFileName);
    }
    
    protected static String getConfigDir(boolean printInstructions) {
        String configDir = System.getProperty(SYS_CONFIG_DIR);
        if (isBlank(configDir)) {
        	configDir = System.getenv(METL_HOME);
        	if (isBlank(configDir)) {
	            configDir = System.getProperty("user.home") + "/.metl";
	            if (printInstructions) {
	                System.out.println("You can configure the following system property to point to a working directory "
	                        + "where configuration files can be found: -D" + SYS_CONFIG_DIR + "=/some/config/dir");
	            }
        	}
        }
        
        if (isBlank(System.getProperty("h2.baseDir"))) {
            System.setProperty("h2.baseDir", configDir);
        }
        
        if (printInstructions) {
            System.out.println("The current config directory is " + configDir);
            System.out.println("The current working directory is " + System.getProperty("user.dir"));
            System.out.println("");
            System.out.println("");
        }
        return configDir;
    }

}
