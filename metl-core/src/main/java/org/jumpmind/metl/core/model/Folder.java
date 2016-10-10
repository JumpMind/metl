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
import java.util.UUID;

public class Folder extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    String type;

    String name;
    
    String projectVersionId;
    
    Folder parent;

    List<Folder> children;
    
    boolean virtualFolder = false;
    
    String rowId = UUID.randomUUID().toString();
    
    boolean deleted = false;

    public Folder() {
    	children = new ArrayList<Folder>();
    }
    
    public Folder(String id) {
    	this();
        setId(id);       
    }
    
    public void makeVirtual() {
        this.virtualFolder = true;
    }
    
    public FolderType getFolderType() {
        return FolderType.valueOf(type);
    }

    public List<Folder> getChildren() {
        return children;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    public Folder getParent() {
        return parent;
    }

    public boolean isParentOf(Folder folder) {
        return folder.getParentFolderId() != null
                && folder.getParentFolderId().equals(getId());
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    public void setParentFolderId(String parentFolderId) {
        if (parentFolderId != null) {
            this.parent = new Folder(parentFolderId);
        } else {
            this.parent = null;
        }
    }
    
    public String getParentFolderId() {
    	return parent != null ? parent.getId() : null;
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return !virtualFolder;
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
