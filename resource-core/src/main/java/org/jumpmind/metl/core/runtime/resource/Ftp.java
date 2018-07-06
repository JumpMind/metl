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
package org.jumpmind.metl.core.runtime.resource;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.sql.Connection;

import org.jumpmind.properties.TypedProperties;

public class Ftp extends AbstractResourceRuntime {

    public static final String TYPE = "Ftp";

    public static final String SERVER = "server";

    public static final String PORT = "port";

    // User is not required since it can be left blank
    public static final String USER = "user";

    // Password is not required since it can be blank.
    public static final String PASSWORD = "password";

    public final static String BASE_PATH = "base.path";

    public static final String CONNECTION_TIMEOUT = "connection.timeout";

    FtpDirectory streamableResource;

    @Override
    protected void start(TypedProperties properties) {
        streamableResource = new FtpDirectory(properties.getProperty(SERVER),
                isNotBlank(properties.getProperty(PORT)) ? properties.getInt(PORT) : null, properties.getProperty(USER),
                properties.getProperty(PASSWORD), properties.getProperty(BASE_PATH), properties.getInt(CONNECTION_TIMEOUT));
    }

    @Override
    public void stop() {
        streamableResource.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) streamableResource;
    }
    
    @Override
    public boolean isTestSupported() {
        return true;
    }
    
    @Override
    public boolean test() {
        try {
            streamableResource.createClient();
            return true;
        } catch (Exception ex) {
            throw new RuntimeException("Error connecting to database", ex);
        } finally {
            streamableResource.close();
        }
    }
}
