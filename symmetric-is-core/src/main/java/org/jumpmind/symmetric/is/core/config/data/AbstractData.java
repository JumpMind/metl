package org.jumpmind.symmetric.is.core.config.data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public abstract class AbstractData implements Serializable {

    private static final long serialVersionUID = 1L;

    String id = UUID.randomUUID().toString();

    Date createTime = new Date();

    String createBy;

    Date lastModifyTime = new Date();

    String lastModifyBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
