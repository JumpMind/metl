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
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;

public class EntityRow implements Serializable {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    Map<String, String> data;
    
    public EntityRow() {
    }

    public EntityRow(String entityName, Map<String, String> row) {
        this.name = entityName;
        this.data = row;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String entityName) {
        this.name = entityName;
    }
    
    public Map<String, String> getData() {
        return data;
    }
    
    public void setData(Map<String, String> row) {
        this.data = row;
    }
    
    public EntityData toEntityData(Model model) {
        EntityData entityData = null;
        for (ModelEntity entity : model.getModelEntities()) {
            for (ModelAttrib attribute : entity.getModelAttributes()) {
                if (name.equals(entity.getName())  && data.containsKey(attribute.getName())) {
                    if (entityData == null) {
                        entityData = new EntityData();
                    }
                    String stringValue = data.get(attribute.getName());
                    entityData.put(attribute.getId(), stringValue);
                }
            }
        }
        return entityData;
    }   
}
