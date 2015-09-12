package org.jumpmind.symmetric.is.core.model;


public class UserGroup extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    String userId;
    
    String groupId;

    public UserGroup() {
    }
    
    public UserGroup(String userId, String groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }
    
    @Override
    public void setName(String name) {
    }

    @Override
    public String getName() {
        return null;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

}