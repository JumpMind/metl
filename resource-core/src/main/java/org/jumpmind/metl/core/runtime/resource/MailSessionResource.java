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

import java.util.HashMap;
import java.util.Map;

import org.jumpmind.properties.TypedProperties;

public class MailSessionResource extends AbstractResourceRuntime {

    public final static String TYPE = "MailSession";

    public static final String SETTING_TRANSPORT = MailSession.SETTING_TRANSPORT;

    public static final String SETTING_HOST_NAME = MailSession.SETTING_HOST_NAME;

    public static final String SETTING_PORT_NUMBER = MailSession.SETTING_PORT_NUMBER;

    public static final String SETTING_USE_TLS = MailSession.SETTING_USE_TLS;

    public static final String SETTING_USE_AUTH = MailSession.SETTING_USE_AUTH;

    public static final String SETTING_USERNAME = MailSession.SETTING_USERNAME;

    public static final String SETTING_PASSWORD = MailSession.SETTING_PASSWORD;

    public static final String SETTING_FROM = MailSession.SETTING_FROM;

    Map<String, String> settings = new HashMap<String, String>();

    MailSession session;

    @Override
    public void stop() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) session;
    }

    @Override
    protected void start(TypedProperties properties) {
        for (Object obj : properties.keySet()) {
            settings.put((String) obj, properties.get((String) obj));
        }
        session = new MailSession(settings);
    }

}
