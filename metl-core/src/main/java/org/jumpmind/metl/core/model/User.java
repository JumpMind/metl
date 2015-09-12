package org.jumpmind.metl.core.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

public class User extends AbstractObjectWithSettings {

    private static final long serialVersionUID = 1L;
    
    String loginId;

    String name;

    String password;
    
    Date lastLoginTime;    
    
    List<Group> groups;
    
    public User() {
        groups = new ArrayList<Group>();
    }

    public static String hashValue(String password) {
        if (password != null) {
            return DigestUtils.sha256Hex(password.getBytes());
        }
        return "";
    }

    @Override
    protected Setting createSettingData() {
        return new UserSetting(id);
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

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
    
    public boolean hasPrivilege(String privilegeName) {
        for (Group group : groups) {
            for (GroupPrivilege priv : group.getGroupPrivileges()) {
                if (privilegeName.equals(priv.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

}
