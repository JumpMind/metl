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

import org.jumpmind.properties.TypedProperties;

public class Sftp extends AbstractResourceRuntime {

    public static final String TYPE = "Sftp";

    public static final String SFTP_SERVER = "sftp.server";
    
    public static final String SFTP_PORT = "sftp.port";
    
    // User is not required since it can be left blank
    public static final String SFTP_USER = "sftp.user";

    // Password is not required since it can be blank.
    public static final String SFTP_PASSWORD = "sftp.password";   
    
    public static final String KEY_FILE_LOCATION = "key.file.location";       
    
    public final static String SFTP_BASE_PATH = "sftp.base.path";

    public static final String SFTP_MUST_EXIST = "sftp.must.exist";
    
    public static final String SFTP_CONNECTION_TIMEOUT = "sftp.connection.timeout";
    
    IDirectory streamableResource;

    @Override
    protected void start(TypedProperties properties) {
        streamableResource = new SftpDirectory(resource,
                properties.getProperty(SFTP_SERVER), 
                properties.getInt(SFTP_PORT),
                properties.getProperty(SFTP_USER),
                properties.getProperty(SFTP_PASSWORD),
                properties.getProperty(KEY_FILE_LOCATION),
                properties.getProperty(SFTP_BASE_PATH),                
                properties.getInt(SFTP_CONNECTION_TIMEOUT),
                properties.is(SFTP_MUST_EXIST));
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
}
