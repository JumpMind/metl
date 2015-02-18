package org.jumpmind.symmetric.is.core.config;

import java.io.Serializable;
import java.util.Date;

import org.jumpmind.symmetric.is.core.config.data.AbstractData;

abstract public class DeprecatedAbstractObject<D extends AbstractData> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    protected D data;
    
    public DeprecatedAbstractObject() {
    }
    
    public DeprecatedAbstractObject(D data) {
        this.data = data;
    }
    
    public D getData() {
        return this.data;
    }
    
    public String getId() {
        return data.getId();
    }
    
    public Date getCreateTime() {
        return data.getCreateTime();
    }

    public void setCreateTime(Date createTime) {
        data.setCreateTime(createTime);
    }

    public String getCreateBy() {
        return data.getCreateBy();
    }

    public void setCreateBy(String createBy) {
        data.setCreateBy(createBy);
    }

    public Date getLastModifyTime() {
        return data.getLastModifyTime();
    }

    public void setLastModifyTime(Date lastModifyTime) {
        data.setLastModifyTime(lastModifyTime);
    }

    public String getLastModifyBy() {
        return data.getLastModifyBy();
    }

    public void setLastModifyBy(String lastModifyBy) {
        data.setLastModifyBy(lastModifyBy);
    }
    
    abstract public void setName(String name);
    
    abstract public String getName();
    
    public boolean isSettingNameAllowed() {
        return false;
    }
    
    @Override
    public int hashCode() {
        return this.data.getId().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeprecatedAbstractObject<?>) {
            return this.data.getId().equals(((DeprecatedAbstractObject<?>)obj).getData().getId());
        } else {
            return super.equals(obj);
        }            
    }
}
