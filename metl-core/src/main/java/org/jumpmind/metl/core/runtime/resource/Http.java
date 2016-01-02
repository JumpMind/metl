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

import static org.jumpmind.metl.core.runtime.resource.HttpDirectory.HTTP_METHOD_GET;
import static org.jumpmind.metl.core.runtime.resource.HttpDirectory.HTTP_METHOD_POST;
import static org.jumpmind.metl.core.runtime.resource.HttpDirectory.HTTP_METHOD_PUT;
import static org.jumpmind.metl.core.runtime.resource.HttpDirectory.SECURITY_BASIC;
import static org.jumpmind.metl.core.runtime.resource.HttpDirectory.SECURITY_NONE;
import static org.jumpmind.metl.core.runtime.resource.HttpDirectory.SECURITY_TOKEN;

import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.properties.TypedProperties;

@ResourceDefinition(typeName = Http.TYPE, resourceCategory = ResourceCategory.STREAMABLE)
public class Http extends AbstractResourceRuntime {

    public static final String TYPE = "Http";

    @SettingDefinition(type = Type.TEXT, order = 10, required = true, label = "URL")
    public static final String URL = "url";

    @SettingDefinition(
            type = Type.CHOICE,
            choices = { HTTP_METHOD_GET, HTTP_METHOD_PUT, HTTP_METHOD_POST },
            order = 20,
            defaultValue = HTTP_METHOD_GET,
            required = true,
            label = "Http Method")
    public static final String HTTP_METHOD = "http.method";
    
    
    @SettingDefinition(
            type = Type.MULTILINE_TEXT,
            order = 25,
            defaultValue = "text/xml; charset=utf-8",
            required = true,
            label = "Content Type")
    public static final String CONTENT_TYPE = "content.type";

    @SettingDefinition(
            type = Type.INTEGER,
            order = 25,
            required = true,
            label = "Http Timeout",
            defaultValue = "60000")
    public static final String HTTP_TIMEOUT = "http.timeout";

    @SettingDefinition(
            type = Type.CHOICE,
            order = 30,
            choices = { SECURITY_NONE, SECURITY_BASIC, SECURITY_TOKEN },
            defaultValue = SECURITY_NONE,
            label = "Security")
    public static final String SECURITY = "security.type";

    @SettingDefinition(type = Type.TEXT, order = 40, required = false, label = "User")
    public static final String SECURITY_USERNAME = "security.usertoken.username";

    @SettingDefinition(type = Type.PASSWORD, order = 50, required = false, label = "Password")
    public static final String SECURITY_PASSWORD = "security.usertoken.password";

    @SettingDefinition(type = Type.PASSWORD, order = 60, required = false, label = "Token")
    public static final String SECURITY_TOKEN_VALUE = "security.usertoken.token";    
    
    HttpDirectory streamable;

    @Override
    protected void start(TypedProperties properties) {
        streamable = new HttpDirectory(properties.get(URL), properties.get(HTTP_METHOD,
                HTTP_METHOD_GET),
                properties.get(CONTENT_TYPE),
                properties.getInt(HTTP_TIMEOUT), properties.get(SECURITY),
                properties.get(SECURITY_USERNAME), properties.get(SECURITY_PASSWORD),
                properties.get(SECURITY_TOKEN_VALUE));
    }

    @Override
    public void stop() {
        streamable.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) streamable;
    }

}
