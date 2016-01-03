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
package org.jumpmind.metl.core.util;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;

public final class LogUtils {
    
    static String logFilePath;
    
    static File logDir;
    
    static boolean fileEnabled;
    
    static boolean consoleEnabled;

    private LogUtils() {
    }
    
    public static boolean isConsoleEnabled() {
        return consoleEnabled;
    }
    
    public static boolean isFileEnabled() {
        return fileEnabled;
    }
    
    public static String getLogFilePath() {
        return logFilePath;
    }
    
    public static void setLogDir(File logDir) {
        LogUtils.logDir = logDir;
    }
    
    public static File getLogDir() {
        return logDir;
    }
    
    public static String formatDuration(long timeInMs) {
        if (timeInMs > 60000) {
            long minutes = timeInMs / 60000;
            long seconds = (timeInMs - (minutes * 60000)) / 1000;
            return minutes + "m " + seconds + "s";
        } else if (timeInMs > 1000) {
            long seconds = timeInMs / 1000;
            return seconds + "s";
        } else {
            return timeInMs + "ms";
        }
    }

    public static void initLogging(String configDir, ApplicationContext ctx) {
                
        /* Optionally remove existing handlers attached to j.u.l root logger */
        SLF4JBridgeHandler.removeHandlersForRootLogger();

        /*
         * Add SLF4JBridgeHandler to j.u.l's root logger, should be done once
         * during the initialization phase of your application
         */
        SLF4JBridgeHandler.install();

        consoleEnabled = Boolean.parseBoolean(ctx.getEnvironment().getProperty(
                EnvConstants.LOG_TO_CONSOLE_ENABLED, "true"));
        if (!consoleEnabled) {
            org.apache.log4j.Logger.getRootLogger().removeAppender("CONSOLE");
        }

        fileEnabled = Boolean.parseBoolean(ctx.getEnvironment().getProperty(
                EnvConstants.LOG_TO_FILE_ENABLED, "true"));
        if (fileEnabled) {
            logFilePath = ctx.getEnvironment().getProperty(EnvConstants.LOG_FILE,(String)null);
            if (isBlank(logFilePath)) {
                logDir = new File(configDir, "logs");
                logDir.mkdirs();
                logFilePath = logDir.getAbsolutePath() + "/metl.log";
            } else {
                logDir = new File(logFilePath).getParentFile();
            }
            try {
                RollingFileAppender logFileAppender = new RollingFileAppender();
                logFileAppender.setFile(logFilePath);
                logFileAppender.setMaxBackupIndex(10);
                logFileAppender.setMaxFileSize("40MB");
                logFileAppender.setAppend(true);
                logFileAppender.setLayout(new PatternLayout("%d %-5p [%c{1}] [%t] %m%n"));
                org.apache.log4j.Logger.getRootLogger().addAppender(logFileAppender);
                logFileAppender.activateOptions();
            } catch (Exception ex) {
                System.err.println("Failed to configure the following log file: " + logFilePath);
                ex.printStackTrace();
            }
        }
    }
    
    public static String normalizeName(String name) {
        if (isNotBlank(name)) {
            if (name.startsWith("<")) {
                name = name.substring(1);
            }
            if (name.endsWith(">")) {
                name = name.substring(0, name.length() - 1);
            }
            return name.replaceAll("[^A-Za-z0-9]", "-").toLowerCase();
        } else {
            return "test";
        }
    }
}
