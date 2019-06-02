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

public class SMB extends AbstractResourceRuntime {

    public static final String TYPE = "SMB";

    public static final String SMB_BASE_URL = "smb.base.url";

    // User is not required since it can be left blank
    public static final String SMB_USER = "smb.user";

    // Password is not required since it can be blank.
    public static final String SMB_PASSWORD = "smb.password";

    // Domain is not required since it can be blank.
    public final static String SMB_DOMAIN = "smb.domain";

    IDirectory streamableResource;

    @Override
    protected void start(TypedProperties properties) {
        streamableResource = new SMBDirectory(properties.getProperty(SMB_BASE_URL), properties.getProperty(SMB_USER),
                properties.getProperty(SMB_PASSWORD), properties.getProperty(SMB_DOMAIN));
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
