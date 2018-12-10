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
