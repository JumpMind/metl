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

public interface IModel {

    public boolean isShared();

    public void setShared(boolean shared);

    public Folder getFolder();

    public void setFolder(Folder folder);
    
    public String getName();

    public void setName(String name);

    public String getFolderId();

    public void setFolderId(String folderId);

    public void setProjectVersionId(String projectVersionId);

    public String getProjectVersionId();
    
    public void setRowId(String rowId);
    
    public String getRowId();

    public boolean isSettingNameAllowed();
    
    public void setDeleted(boolean deleted);
    
    public boolean isDeleted();
    
    public String getId();
    
    public String getType();

}
