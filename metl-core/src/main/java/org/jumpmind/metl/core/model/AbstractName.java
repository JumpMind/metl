package org.jumpmind.metl.core.model;

import java.util.UUID;


abstract public class AbstractName extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    boolean deleted;
    
    boolean shared;
    
    String projectVersionId;
    
    String rowId = UUID.randomUUID().toString();
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setShared(boolean shared) {
        this.shared = shared;
    }
    
    public boolean isShared() {
        return shared;
    }
    
    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
    }
    
    public String getProjectVersionId() {
        return projectVersionId;
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
    
    public void setRowId(String rowId) {
        this.rowId = rowId;
    }
    
    public String getRowId() {
        return rowId;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractName && obj.getClass().equals(getClass())) {
            return id.equals(((AbstractName)obj).getId());
        } else {
            return super.equals(obj);
        }            
    }

}
