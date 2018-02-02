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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;

public class EntityTable implements Serializable {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
    
    public EntityTable(String entityName) {
        this.name = entityName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, String>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, String>> rows) {
        this.rows = rows;
    }
    
    public EntityData toEntityData(Model model) {
        
        EntityData entityData = new EntityData();
        ModelEntity entity = model.getEntityByName(name);
        for (Map<String, String> row:rows) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                entityData.put(entity.getModelAttributeByName(entry.getKey()).getId(),entry.getValue());
            }
        }
        return entityData;
    }   
}
