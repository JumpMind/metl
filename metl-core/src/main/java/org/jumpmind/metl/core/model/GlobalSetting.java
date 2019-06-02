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
package org.jumpmind.metl.core.model;

public class GlobalSetting extends Setting {

    private static final long serialVersionUID = 1L;
    
    public static final String SYSTEM_TEXT = "system.text";
    
    public static final String AUDIT_EVENT_RETENTION_IN_DAYS = "audit.event.retention.days";
    
    public static final String PASSWORD_MIN_LENGTH = "password.min.length";
    public static final String PASSWORD_PROHIBIT_PREVIOUS = "password.prohibit.previous";
    public static final String PASSWORD_PROHIBIT_COMMON_WORDS = "password.prohibit.common.words";
    public static final String PASSWORD_REQUIRE_ALPHANUMERIC = "password.require.alphanumeric";
    public static final String PASSWORD_REQUIRE_SYMBOL = "password.require.symbol";
    public static final String PASSWORD_REQUIRE_MIXED_CASE = "password.require.mixed.case";
    public static final String PASSWORD_EXPIRE_DAYS = "password.expire.days";
    public static final int PASSWORD_EXPIRE_DAYS_DEFAULT = 60;
    public static final String PASSWORD_FAILED_ATTEMPTS = "password.failed.attempts";
    public static final int PASSWORD_FAILED_ATTEMPTS_DEFAULT = 3;
    
    public static final String CONFIG_BACKUP_ENABLED = "config.backup.enabled";
    public static final String CONFIG_BACKUP_CRON = "config.backup.cron";
    public static final String CONFIG_BACKUP_RETENTION_IN_DAYS = "config.backup.retention.days";
    
    public static final boolean DEFAULT_CONFIG_BACKUP_ENABLED = true;
    public static final String DEFAULT_CONFIG_BACKUP_CRON = "0 0 0 * * *";
    public static final int DEFAULT_CONFIG_BACKUP_RETENTION_IN_DAYS = 30;
    
    public static final int DEFAULT_AUDIT_EVENT_RETENTION_IN_DAYS = 30;

    public final static String LDAP_HOST = "console.auth.ldap.host";
    public final static String LDAP_BASE_DN = "console.auth.ldap.baseDN";
    public final static String LDAP_SEARCH_ATR = "console.auth.ldap.searchAttribute";
    public final static String LDAP_SAMPLE_USR = "console.auth.ldap.sampleUser";
    public final static String LDAP_SECURITY_PRINCIPAL = "console.auth.ldap.securityPrincipal";
    public final static String LDAP_SECURITY_PRINCIPAL_DEFAULT = "${searchAttribute}=${username},${baseDN}";

    @Override
    public String toString() {
        return "global setting {" + name + ":" + value + "}";
    }
    
}
