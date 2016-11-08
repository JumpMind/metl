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


abstract public class AbstractName extends AbstractNamedObject implements Comparable<AbstractName> {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    boolean deleted;
    
    String projectVersionId;
    
    String rowId = UUID.randomUUID().toString();
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
    }
    
    public String getProjectVersionId() {
        return projectVersionId;
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
    
    public void setRowId(String rowId) {
        this.rowId = rowId;
    }
    
    public String getRowId() {
        return rowId;
    }
    
    @Override
    public int compareTo(AbstractName o) {
        if (name != null) {
            return name.compareTo(o.name);
        } else {
            return 0;
        }
    }

}
