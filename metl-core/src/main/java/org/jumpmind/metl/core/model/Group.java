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

public class Group extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    String name;
    
    boolean readOnly;
    
    List<GroupPrivilege> groupPrivileges;

    public Group() {
        groupPrivileges = new ArrayList<GroupPrivilege>();
    }
    
    public Group(String name) {
        this();
        this.name = name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }

    public List<GroupPrivilege> getGroupPrivileges() {
        return groupPrivileges;
    }
    
    public boolean hasPrivilege(Privilege privilegeName) {
        for (GroupPrivilege priv : groupPrivileges) {
            if (privilegeName.name().equalsIgnoreCase(priv.getName())) {
                return true;
            }
        }
        return false;
    }

    public void setGroupPrivileges(List<GroupPrivilege> groupPrivileges) {
        this.groupPrivileges = groupPrivileges;
    }

}
