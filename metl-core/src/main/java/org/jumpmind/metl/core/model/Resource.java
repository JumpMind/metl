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

public class Resource extends AbstractObjectWithSettings implements IAuditable {

    private static final long serialVersionUID = 1L;

    Folder folder;

    String name;

    String type;

    String projectVersionId;
    
    boolean deleted = false;
    
    String rowId = UUID.randomUUID().toString();

    public Resource() {
    }

    public Resource(String id) {
        setId(id);
    }

    public Resource(Folder folder, Setting... settings) {
        super(settings);
        setFolder(folder);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFolderId(String folderId) {
        if (folderId != null) {
            folder = new Folder(folderId);
        } else {
            folder = null;
        }
    }

    public String getFolderId() {
        return folder != null ? folder.getId() : null;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    protected Setting createSettingData() {
        return new ResourceSetting(getId());
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
    }

    public String getProjectVersionId() {
        return projectVersionId;
    }
    
    public void setRowId(String rowId) {
        this.rowId = rowId;
    }
    
    public String getRowId() {
        return rowId;
    }
    
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
}
