package org.jumpmind.symmetric.is.core.model;

public class GroupPrivilege extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String name;
    
    String groupId;

    public GroupPrivilege() {
    }
    
    public GroupPrivilege(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

}
