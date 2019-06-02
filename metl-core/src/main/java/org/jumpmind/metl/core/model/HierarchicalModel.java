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

import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.runtime.EntityData;

public class HierarchicalModel extends AbstractModel implements IModel {

    private static final long serialVersionUID = 1L;

    public static final String TYPE = "HIERARCHICAL";

    ModelSchemaObject rootObject;
    
    public HierarchicalModel() {
        
    }
    
    public HierarchicalModel(String id) {
        super(id);
    }
    
    public Row toRow(EntityData data) {
        //TODO:
        return null;
    }

    public ModelSchemaObject getRootObject() {
        return rootObject;
    }

    public void setRootObject(ModelSchemaObject rootObject) {
        this.rootObject = rootObject;
    }
    
    public ModelSchemaObject getObjectById(String objectId) {
        return getObjectById(rootObject, objectId);
    }
    
    private ModelSchemaObject getObjectById(ModelSchemaObject object, String objectId) {
        ModelSchemaObject objectToReturn = null;
        for (ModelSchemaObject childObject : object.getChildObjects()) {
            if (childObject.getId().equals(objectId)) {
                objectToReturn = childObject;
            }
        }
        if (objectToReturn == null) {
            for (ModelSchemaObject childObject : object.getChildObjects()) {
                getObjectById(childObject, objectId);
            }
        }
        return objectToReturn;
    }

    @Override
    public String getType() {
        return TYPE;
    }

}
