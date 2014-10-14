package org.jumpmind.symmetric.is.core.config.data;

import java.io.Serializable;
import java.util.Date;

public class ComponentFlowNodeLinkData implements Serializable  {

    private static final long serialVersionUID = 1L;

    String sourceNodeId;
    
    String targetNodeId;
    
    Date createTime = new Date();

    String createBy;

    Date lastModifyTime;

    String lastModifyBy;
    
    public ComponentFlowNodeLinkData() {
    }
    
    public ComponentFlowNodeLinkData(String sourceNodeId, String targetNodeId) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Date lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public String getLastModifyBy() {
        return lastModifyBy;
    }

    public void setLastModifyBy(String lastModifyBy) {
        this.lastModifyBy = lastModifyBy;
    }
    
    
}
