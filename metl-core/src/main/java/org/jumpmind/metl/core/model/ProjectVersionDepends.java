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

import java.util.UUID;

public class ProjectVersionDepends extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    String projectVersionId;
    
    String targetProjectVersionId;
    
    String rowId;
    
    transient ProjectVersion targetProjectVersion;
    
    @Override
    public void setName(String name) {
    }
    
    @Override
    public String getName() {
        if (targetProjectVersion != null) {
            return String.format("%s (%s)", targetProjectVersion.getProject().getName(),
                    targetProjectVersion.getName());
        } else {
            return targetProjectVersionId;
        }
    }
    
    public void setTargetProjectVersion(ProjectVersion targetProjectVersion) {
        this.targetProjectVersion = targetProjectVersion;
        this.targetProjectVersionId = targetProjectVersion.getId();
    }

    /**
     * @return the sourceProjectVersionId
     */
    public String getProjectVersionId() {
        return projectVersionId;
    }

    /**
     * @param sourceProjectVersionId the sourceProjectVersionId to set
     */
    public void setProjectVersionId(String sourceProjectVersionId) {
        this.projectVersionId = sourceProjectVersionId;
    }

    /**
     * @return the targetProjectVersionId
     */
    public String getTargetProjectVersionId() {
        return targetProjectVersionId;
    }

    /**
     * @param targetProjectVersionId the targetProjectVersionId to set
     */
    public void setTargetProjectVersionId(String targetProjectVersionId) {
        this.targetProjectVersionId = targetProjectVersionId;
    }
    

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }
    
    public String getRowId() {
        if (rowId == null) {
            rowId = UUID.randomUUID().toString();
        }
        return rowId;
    }

}
