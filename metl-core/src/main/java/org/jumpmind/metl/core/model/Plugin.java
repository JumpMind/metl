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

public class Plugin implements Serializable, Comparable<Plugin> {

    private static final long serialVersionUID = 1L;

    int loadOrder;
    String artifactName;
    String artifactGroup;
    String artifactVersion;
    Date createTime = new Date();
    String createBy;
    Date lastUpdateTime = new Date();
    String lastUpdateBy;
    
    public Plugin() {
    }
        
    public Plugin(String artifactGroup, String artifactName) {
        this();
        this.artifactName = artifactName;
        this.artifactGroup = artifactGroup;
    }
    
    public Plugin(String artifactGroup, String artifactName, String artifactVersion) {
        this(artifactGroup, artifactName);
        this.artifactVersion = artifactVersion;
    }
        
    public Plugin(String artifactGroup, String artifactName, String artifactVersion, int loadOrder) {
        this(artifactGroup, artifactName, artifactVersion);
        this.loadOrder = loadOrder;
    }
    
    public Plugin(String artifactGroup, String artifactName, int loadOrder) {
        this(artifactGroup, artifactName, null, loadOrder);
    }

    public boolean matches(Plugin plugin) {
        return matches(plugin.getArtifactGroup(), plugin.getArtifactName());
    }
    
    public boolean matches(String artifactGroup, String artifactName) {
        boolean matches = false;
        if (this.artifactGroup != null && this.artifactGroup.equals(artifactGroup)) {
            if (this.artifactName != null && this.artifactName.equals(artifactName)) {
                matches = true;
            }
        }
        return matches;
    }

    public void setArtifactGroup(String artifactGroup) {
        this.artifactGroup = artifactGroup;
    }

    public String getArtifactGroup() {
        return artifactGroup;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }
    
    public int getLoadOrder() {
        return loadOrder;
    }
    
    public void setLoadOrder(int loadOrder) {
        this.loadOrder = loadOrder;
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

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getLastUpdateBy() {
        return lastUpdateBy;
    }

    public void setLastUpdateBy(String lastUpdateBy) {
        this.lastUpdateBy = lastUpdateBy;
    }

    @Override
    public int compareTo(Plugin o) {
        int value = 0;
        if (o != null) {
            value = this.artifactGroup.compareTo(o.artifactGroup);
            if (value == 0) {
                value = this.artifactName.compareTo(o.artifactName);
                if (value == 0 && this.artifactVersion != null) {
                    value = this.artifactVersion.compareTo(o.artifactVersion);
                }
            }
        } else {
            value = 1;    
        }
        return value;        
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactGroup == null) ? 0 : artifactGroup.hashCode());
        result = prime * result + ((artifactName == null) ? 0 : artifactName.hashCode());
        result = prime * result + ((artifactVersion == null) ? 0 : artifactVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Plugin other = (Plugin) obj;
        if (artifactGroup == null) {
            if (other.artifactGroup != null)
                return false;
        } else if (!artifactGroup.equals(other.artifactGroup))
            return false;
        if (artifactName == null) {
            if (other.artifactName != null)
                return false;
        } else if (!artifactName.equals(other.artifactName))
            return false;
        if (artifactVersion == null) {
            if (other.artifactVersion != null)
                return false;
        } else if (!artifactVersion.equals(other.artifactVersion))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s:%s", artifactGroup, artifactName, artifactVersion);
    }
    
    

}
