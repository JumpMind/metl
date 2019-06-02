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

import java.util.ArrayList;
import java.util.List;

public class Project extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    String name;

    String description;

    List<ProjectVersion> projectVersions;
    
    List<Tag> tags;

    boolean deleted;

    public Project() {
        this.projectVersions = new ArrayList<ProjectVersion>();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public List<ProjectVersion> getProjectVersions() {
        return projectVersions;
    }

    public void setProjectVersions(List<ProjectVersion> projectVersions) {
        this.projectVersions = projectVersions;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }    
    
    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public ProjectVersion getMasterVersion() {
        ProjectVersion version = null;
        for (ProjectVersion projectVersion : projectVersions) {
            if (version == null || projectVersion.isMaster()) {
                version = projectVersion;
            }
        }
        return version;
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
}
