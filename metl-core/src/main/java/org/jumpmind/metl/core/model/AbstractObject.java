/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

abstract public class AbstractObject implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    
    private String id;

    Date createTime = new Date();

    String createBy;

    Date lastUpdateTime = new Date();

    String lastUpdateBy;
    
    public AbstractObject() {
    }
    
    public boolean isSettingNameAllowed() {
        return false;
    }
    
    public String getId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
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

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastModifyTime) {
        this.lastUpdateTime = lastModifyTime;
    }

    public String getLastUpdateBy() {
        return lastUpdateBy;
    }

    public void setLastUpdateBy(String lastModifyBy) {
        this.lastUpdateBy = lastModifyBy;
    }
    
    @Override
    public int hashCode() {
        return getId().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractObject && obj.getClass().equals(getClass())) {
            return getId().equals(((AbstractObject)obj).getId());
        } else {
            return super.equals(obj);
        }            
    }
    
    public AbstractObject clone() {
        try {
            return (AbstractObject)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
    
}
