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

/**
 * These are environment properties definitions that can be set in the metl.properties at startup
 */
public final class EnvConstants {

    private EnvConstants() {
    }
    
    public final static String LOG_FILE = "log.file";
    
    public final static String LOG_TO_CONSOLE_ENABLED = "log.to.console.enabled";
    
    public final static String LOG_TO_FILE_ENABLED = "log.to.file.enabled";
    
    public final static String TABLE_PREFIX = "table.prefix";
    
    public final static String CONFIG_DIR = "config.dir";
    
    public static final String LOG_FILE_RETENTION_IN_DAYS = "log.file.retention.days";
    
    public static final String LOG_FILE_MAX_SIZE = "log.file.max.size";
    
    public static final String LOG_FILE_PATTERN_LAYOUT = "log.file.pattern.layout";
    
}
