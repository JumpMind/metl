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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User extends AbstractObjectWithSettings {

    private static final long serialVersionUID = 1L;
    
    public final static String AUTH_METHOD_INTERNAL = "INTERNAL";
    public final static String AUTH_METHOD_LDAP = "LDAP";
    
    String loginId;

    String name;

    String password;
    
    String authMethod = AUTH_METHOD_INTERNAL;
    
    String salt;
    
    Date lastPasswordTime;
    
    Date lastLoginTime;    
    
    List<Group> groups;
    
    int failedLogins;
    
    public User() {
        groups = new ArrayList<Group>();
    }

    @Override
    protected Setting createSettingData() {
        return new UserSetting(getId());
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }
    
    public String getAuthMethod() {
        return authMethod;
    }
    
    public void setLastPasswordTime(Date lastPasswordTime) {
        this.lastPasswordTime = lastPasswordTime;
    }
    
    public Date getLastPasswordTime() {
        return lastPasswordTime;
    }
    
   public void setSalt(String salt) {
    this.salt = salt;
   }
   
   public String getSalt() {
    return salt;
   }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
    
    public boolean hasPrivilege(String privilegeName) {
        for (Group group : groups) {
            for (GroupPrivilege priv : group.getGroupPrivileges()) {
                if (privilegeName.equalsIgnoreCase(priv.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

	public int getFailedLogins() {
		return failedLogins;
	}
	
	public void setFailedLogins(int failedLogins) {
		this.failedLogins = failedLogins;
	}
    
	@Override
	public String toString() {
		return loginId;
	}

}
