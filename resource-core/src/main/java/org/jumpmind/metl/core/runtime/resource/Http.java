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

import java.io.IOException;
import java.net.HttpURLConnection;

import org.jumpmind.properties.TypedProperties;

public class Http extends AbstractResourceRuntime {

    public static final String TYPE = "Http";

    public static final String URL = "url";

    public static final String HTTP_METHOD = "http.method";
    
    public static final String CONTENT_TYPE = "content.type";

    public static final String HTTP_TIMEOUT = "http.timeout";

    public static final String SECURITY = "security.type";

    public static final String SECURITY_USERNAME = "security.usertoken.username";

    public static final String SECURITY_PASSWORD = "security.usertoken.password";

    public static final String SECURITY_TOKEN_VALUE = "security.usertoken.token";    
    
    public static final String SECURITY_OAUTH10_CONSUMER_KEY = "security.oauth10.consumer.key";
    
    public static final String SECURITY_OAUTH10_CONSUMER_SECRET = "security.oauth10.consumer.secret";
    
    public static final String SECURITY_OAUTH10_TOKEN = "security.oauth10.token";
    
    public static final String SECURITY_OAUTH10_TOKEN_SECRET = "security.oauth10.token.secret";
    
    public static final String SECURITY_OAUTH10_VERSION = "security.oauth10.version";
    
    public static final String SECURITY_OAUTH10_SIGNATURE_METHOD = "security.oauth10.signature.method";
    
    public static final String SECURITY_OAUTH10_REALM = "security.oauth10.realm";
    
    HttpDirectory streamable;

    @Override
    protected void start(TypedProperties properties) {
        streamable = new HttpDirectory(properties.get(URL), properties.get(HTTP_METHOD,
                HTTP_METHOD_GET),
                properties.get(CONTENT_TYPE),
                properties.getInt(HTTP_TIMEOUT), properties.get(SECURITY),
                properties.get(SECURITY_USERNAME), properties.get(SECURITY_PASSWORD),
                properties.get(SECURITY_TOKEN_VALUE), 
                properties.get(SECURITY_OAUTH10_CONSUMER_KEY), properties.get(SECURITY_OAUTH10_CONSUMER_SECRET),
                properties.get(SECURITY_OAUTH10_TOKEN), properties.get(SECURITY_OAUTH10_TOKEN_SECRET),
                properties.get(SECURITY_OAUTH10_VERSION), properties.get(SECURITY_OAUTH10_SIGNATURE_METHOD),
                properties.get(SECURITY_OAUTH10_REALM));
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
    
    @Override
    public boolean isTestSupported() {
        return true;
    }
    
    @Override
    public boolean test() {
        return streamable.test();
    }

}
