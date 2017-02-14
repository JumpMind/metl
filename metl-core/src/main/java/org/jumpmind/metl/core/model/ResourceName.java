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

public class ResourceName extends AbstractName {

    private static final long serialVersionUID = 1L;

    protected String type;

    public ResourceName() {
    }

    public ResourceName(Resource obj) {
        this.type = obj.getType();
        this.name = obj.getName();
        this.setId(obj.getId());
        this.rowId = obj.getRowId();
        this.createTime = obj.getCreateTime();
        this.createBy = obj.getCreateBy();
        this.lastUpdateBy = obj.getLastUpdateBy();
        this.lastUpdateTime = obj.getLastUpdateTime();
        this.projectVersionId = obj.getProjectVersionId();
        this.deleted = obj.isDeleted();
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
