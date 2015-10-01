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

import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

public class MailSession {

    public static final String SETTING_HOST_NAME = "mail.host";
    
    public static final String SETTING_TRANSPORT = "mail.transport";
    
    public static final String SETTING_PORT_NUMBER = "mail.port";
    
    public static final String SETTING_FROM = "mail.from";
    
    public static final String SETTING_USERNAME = "mail.user";
    
    public static final String SETTING_PASSWORD = "mail.password";
    
    public static final String SETTING_USE_TLS = "mail.smtp.starttls.enable";
    
    public static final String SETTING_USE_AUTH = "mail.smtp.auth";

    Session session;
    
    Transport transport;
    
    Map<String, String> globalSettings;
    
    public MailSession(Map<String, String> globalSettings) {
        this.globalSettings = globalSettings;

        Properties prop = new Properties();
        prop.setProperty(SETTING_HOST_NAME, getGlobalSetting(SETTING_HOST_NAME, "localhost"));
        prop.setProperty(SETTING_PORT_NUMBER, getGlobalSetting(SETTING_PORT_NUMBER, "25"));
        prop.setProperty(SETTING_FROM, getGlobalSetting(SETTING_FROM, "metl@localhost"));
        prop.setProperty(SETTING_USE_TLS, getGlobalSetting(SETTING_USE_TLS, "false"));
        prop.setProperty(SETTING_USE_AUTH, getGlobalSetting(SETTING_USE_AUTH, "false"));

        session = Session.getInstance(prop);
    }

    public Transport getTransport() throws MessagingException {
        if (transport == null || !transport.isConnected()) {
            transport = session.getTransport(getGlobalSetting(SETTING_TRANSPORT, "smtp"));
    
            if (Boolean.parseBoolean(getGlobalSetting(SETTING_USE_AUTH, "false"))) {
                transport.connect(globalSettings.get(SETTING_USERNAME), globalSettings.get(SETTING_PASSWORD));
            } else {
                transport.connect();
            }
        }
        return transport;
    }
    
    public void closeTransport(Transport transport) {
        try {
            if (transport != null) {
                transport.close();
                transport = null;
            }
        } catch (MessagingException e) {
        }
    }

    public Session getSession() {
        return session;
    }

    private String getGlobalSetting(String name, String defaultValue) {
        String value = globalSettings.get(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

}
