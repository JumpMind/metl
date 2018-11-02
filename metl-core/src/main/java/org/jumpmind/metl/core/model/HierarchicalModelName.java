package org.jumpmind.metl.core.model;

public class HierarchicalModelName extends AbstractName {

    
    private static final long serialVersionUID = 1L;
    
    public HierarchicalModelName() {
    }

    public HierarchicalModelName(HierarchicalModel obj) {
        this.name = obj.getName();
        this.setId(obj.getId());
        this.rowId = obj.getRowId();
        this.createTime = obj.getCreateTime();
        this.createBy = obj.getCreateBy();
        this.lastUpdateBy = obj.getLastUpdateBy();
        this.lastUpdateTime = obj.getLastUpdateTime();
        this.projectVersionId = obj.getProjectVersionId();
        this.deleted = obj.isDeleted();
    }

}
