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

import java.text.SimpleDateFormat;
import java.util.Date;

public class ProjectVersion extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    public enum VersionType { 
        MASTER, BRANCH, RELEASE 
    }
        
    Project project;

    String description = "";

    String origVersionId;

    String versionLabel;
    
    String versionType;

    Date releaseDate;

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
        this.versionLabel = name;
    }

    @Override
    public String getName() {
        return versionLabel;
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

    public boolean locked() {
        return isReleased() || archived || deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted || project.isDeleted();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    public String getVersionType() {
        return versionType;
    }

    public void setVersionType(String versionType) {
        this.versionType = versionType;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public boolean isReleased() {
        return releaseDate != null;
    }
    
    public String attemptToCalculateNextVersionLabel() {
        return attemptToCalculateNextVersionLabel(versionLabel);
    }
    
    public boolean isMaster() {
        return VersionType.MASTER.toString().equalsIgnoreCase(versionType);
    }
        
    public static String attemptToCalculateNextVersionLabel(String versionLabel) {
        String datetime = new SimpleDateFormat("yyyyMMddmmhhss").format(new Date());
        String nextVersion = versionLabel + "." + datetime;
        int index = versionLabel.lastIndexOf(".");
        if (index >= 0) {
            String prefix = versionLabel.substring(0, index+1);
            String suffix = versionLabel.substring(index + 1, versionLabel.length());
            try {
                suffix = Integer.toString(Integer.parseInt(suffix) + 1);
                if (suffix.length() == datetime.length()) {
                    suffix = datetime;
                }

            } catch (NumberFormatException e) {
                suffix = datetime;
            }
            nextVersion = prefix + suffix;
        }
        return nextVersion;
    }
}
