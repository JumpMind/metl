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
package org.jumpmind.metl;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Wrapper {

    protected static final String SYS_CONFIG_DIR = "org.jumpmind.metl.ui.init.config.dir";
    protected static final String METL_HOME = "METL_HOME";
    protected static final String METL_SERVICE_CONF = "metl_service.conf";

    private static String configDir;
    
    public static void runServiceWrapper(String[] args) throws Exception {

        ProtectionDomain protectionDomain = Wrapper.class.getProtectionDomain();
        String jarFileName=protectionDomain.getCodeSource().getLocation().getFile();
        
        String appHomeDir = getConfigDir(args, true);
        createConfigFileIfNeeded(appHomeDir, jarFileName);
        createLogDirIfNeeded(appHomeDir);

        String[] wrapperArgs = new String[] { args[0], new File(appHomeDir, METL_SERVICE_CONF).getAbsolutePath(), appHomeDir };
        org.jumpmind.symmetric.wrapper.Wrapper.main(wrapperArgs);
    }
    
    protected static void createLogDirIfNeeded(String appHomeDir) {
        
        Path logFilePath = Paths.get(appHomeDir + "/logs");
        if (!Files.exists(logFilePath)) {
            try {
                Files.createDirectories(logFilePath);
            } catch (IOException e) {
                System.out.println("Unable to create log file directory " + logFilePath + "Error =" + e.getMessage());
                System.exit(-1);
            }
        }
    }
    
    protected static void createConfigFileIfNeeded(String appHomeDir, String jarFileName) {
        
        File configFile = new File(appHomeDir, METL_SERVICE_CONF);
        if (!configFile.exists()) {   
            try {
                String propContent = IOUtils.toString(Wrapper.class.getClassLoader().getResourceAsStream(METL_SERVICE_CONF));
                propContent = propContent.replace("$(metl.war)", jarFileName);
                propContent = propContent.replace("$(java.io.tmpdir)", appHomeDir + File.separator + "tmp");     
                propContent = propContent.replace("$(metl.home.dir)", appHomeDir);
                FileUtils.write(configFile, propContent);
            } catch (Exception e) {
                System.out.println("Unable to write config file for service wrapper." + e.getMessage());
                System.exit(-1);
            }
        }
    }
    
    protected static String getConfigDir(String[] args, boolean printInstructions) {

        if (configDir == null) {
            configDir = "";
            
            if (args != null && args.length > 1 && !args[1].equalsIgnoreCase("INSTALL")) {
                int index = args[1].lastIndexOf(File.separator);
                if (index != -1) {
                    configDir = args[1].substring(0, index + 1);
                }
            } else {
                configDir = System.getProperty(SYS_CONFIG_DIR);
                if (isBlank(configDir)) {
                    configDir = System.getenv(METL_HOME);
                    if (isBlank(configDir)) {
                        configDir = System.getProperty("user.dir");
                        if (printInstructions) {
                            System.out.println("You can configure the following system property to point to a working directory "
                                    + "where configuration files can be found: -D" + SYS_CONFIG_DIR + "=/some/config/dir");
                        }
                    }
                }
                if (printInstructions) {
                    System.out.println("The current config directory is " + configDir);
                    System.out.println("The current working directory is " + System.getProperty("user.dir"));
                    System.out.println("");
                    System.out.println("");
                }
            }    
            if (isBlank(System.getProperty("h2.baseDir"))) {
                System.setProperty("h2.baseDir", configDir);
            }
        }        
        return configDir;
    }
}