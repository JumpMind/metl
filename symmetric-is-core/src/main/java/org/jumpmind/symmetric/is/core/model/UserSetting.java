package org.jumpmind.symmetric.is.core.model;

public class UserSetting extends Setting {

    private static final long serialVersionUID = 1L;

    String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
}
