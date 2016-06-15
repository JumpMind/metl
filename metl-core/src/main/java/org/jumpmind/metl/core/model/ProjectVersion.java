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

public class ProjectVersion extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Project project;
    
    String description = "";

    String origVersionId;
    
    String versionLabel;
    
    boolean workingVersion;

    boolean readOnly;

    boolean archived;
    
    boolean deleted;
    
    public ProjectVersion(String id) {
        setId(id);
    }
    
    public ProjectVersion() {
    }
    
    
    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
    
    public String getVersionLabel() {
        return versionLabel;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getName() {
        return String.format("%s (%s)", project.getName(), versionLabel);
    }

    public void setProjectId(String projectId) {
        if (projectId != null) {
            project = new Project();
            project.setId(projectId);
        } else {
            project = null;
        }
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    public Project getProject() {
        return project;
    }

    public String getProjectId() {
        return project != null ? project.getId() : null;
    }

    public void setOrigVersionId(String origProjectId) {
        this.origVersionId = origProjectId;
    }

    public String getOrigVersionId() {
        return origVersionId;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setReadOnly(boolean locked) {
        this.readOnly = locked;
    }

    public boolean isReadOnly() {
        return readOnly;
    }   

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setWorkingVersion(boolean workingVersion) {
        this.workingVersion = workingVersion;
    }
    
    public boolean isWorkingVersion() {
        return workingVersion;
    }
}
