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

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

public class ProjectVersionPlugin extends Plugin implements Serializable {

    private static final long serialVersionUID = 1L;
    
    static final GenericVersionScheme versionScheme = new GenericVersionScheme(); 

    String projectVersionId;
    String definitionType;
    String definitionTypeId;
    String definitionName;
    String latestArtifactVersion;
    boolean enabled = true;
    boolean pinVersion = false;

    public ProjectVersionPlugin() {
    }
    
    public void setDefinitionName(String definitionName) {
        this.definitionName = definitionName;
    }
    
    public String getDefinitionName() {
        return definitionName;
    }
    
    public void setDefinitionType(String definitionType) {
        this.definitionType = definitionType;
    }
    
    public String getDefinitionType() {
        return definitionType;
    }
    
    public void setDefinitionTypeId(String definitionTypeId) {
        this.definitionTypeId = definitionTypeId;
    }
    
    public String getDefinitionTypeId() {
        return definitionTypeId;
    }
    
    public String getProjectVersionId() {
        return projectVersionId;
    }

    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
    }


    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactGroup() {
        return artifactGroup;
    }

    public void setArtifactGroup(String artifactGroup) {
        this.artifactGroup = artifactGroup;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getLatestArtifactVersion() {
        return latestArtifactVersion;
    }

    public void setLatestArtifactVersion(String latestArtifactVersion) {
        this.latestArtifactVersion = latestArtifactVersion;
    }
    
    public boolean isUpdateAvailable() {
        try {
            if (latestArtifactVersion != null && artifactVersion != null) {
                Version latest = versionScheme.parseVersion(latestArtifactVersion);
                Version current = versionScheme.parseVersion(artifactVersion);
                return current.compareTo(latest) < 0;
            } else {
                return false;
            }
        } catch (InvalidVersionSpecificationException e) {            
            return false;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public void setPinVersion(boolean pinVersion) {
        this.pinVersion = pinVersion;
    }
    
    public boolean isPinVersion() {
        return pinVersion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((definitionType == null) ? 0 : definitionType.hashCode());
        result = prime * result + ((definitionTypeId == null) ? 0 : definitionTypeId.hashCode());
        result = prime * result + ((projectVersionId == null) ? 0 : projectVersionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProjectVersionPlugin other = (ProjectVersionPlugin) obj;
        if (definitionType == null) {
            if (other.definitionType != null)
                return false;
        } else if (!definitionType.equals(other.definitionType))
            return false;
        if (definitionTypeId == null) {
            if (other.definitionTypeId != null)
                return false;
        } else if (!definitionTypeId.equals(other.definitionTypeId))
            return false;
        if (projectVersionId == null) {
            if (other.projectVersionId != null)
                return false;
        } else if (!projectVersionId.equals(other.projectVersionId))
            return false;
        return true;
    }


    
    

}
