package org.jumpmind.symmetric.is.core.model;

public class UserSetting extends Setting {

    private static final long serialVersionUID = 1L;
    
    public static final String SETTING_CURRENT_PROJECT_ID_LIST = "current.project.ids";

    String userId;
    
    public UserSetting() {
    }
    
    public UserSetting(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
}
