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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.metl.core.model.User;

import com.vaadin.server.VaadinSession;

public class AppSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private static List<AppSession> appSessions = new ArrayList<>();

    public static synchronized List<AppSession> getAppSessions() {
        return new ArrayList<>(appSessions);
    }

    public static synchronized void addAppSession(AppSession appSession) {
        appSessions.add(appSession);
    }

    public static synchronized void remove(AppSession appSession) {
        appSessions.remove(appSession);
    }

    User user;

    String remoteUser;

    String remoteAddress;

    String remoteHost;
    
    String userAgent;

    VaadinSession vaadinSession;

    Date loginTime;

    public AppSession(String remoteUser, String remoteAddress, String remoteHost,
            VaadinSession vaadinSession, String userAgent, Date createTime) {
        this.remoteUser = remoteUser;
        this.remoteAddress = remoteAddress;
        this.remoteHost = remoteHost;
        this.vaadinSession = vaadinSession;
        this.loginTime = createTime;
        this.userAgent = userAgent;
    }
    
    public boolean equals(Object o) {
        if (o != null && o instanceof AppSession) {
            AppSession appSession = (AppSession) o;
            if (vaadinSession.getSession() == null || appSession.getVaadinSession().getSession() == null) {
                return user.getLoginId().equals(appSession.user.getLoginId())
                        && remoteAddress.equals(appSession.remoteAddress) && loginTime.equals(appSession.loginTime);
            } else {
                return vaadinSession.getSession().getId().equals(((AppSession) o).vaadinSession.getSession().getId());
            }
        }
        return false;
    }
    
    public void setUser(User user) {
        this.user = user;
    }

    public String getLoginId() {
        return user.getLoginId();
    }

    /**
     * @return the remoteUser
     */
    public String getRemoteUser() {
        return remoteUser;
    }

    /**
     * @return the remoteAddress
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * @return the remoteHost
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    public Date getLastActivity() {
        return new Date(vaadinSession.getLastRequestTimestamp());
    }

    /**
     * @return the createTime
     */
    public Date getLoginTime() {
        return loginTime;
    }
    
    public String getUserAgent() {
        return userAgent;
    }

    public VaadinSession getVaadinSession() {
        return vaadinSession;
    }
}
