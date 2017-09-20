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
package org.jumpmind.metl.core.util;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;

final public class ComponentUtils {

    private ComponentUtils() {
    }

    public static Object getAttributeValue(Model model, List<EntityData> rows, String entityName, String attributeName) {
        List<Object> values = getAttributeValues(model, rows, entityName, attributeName);
        if (values.size() > 0) {
            return values.get(0);
        }
        return null;
    }
    
    public static Object getAttributeValue(Model model, EntityData data, String entityName, String attributeName) {
        ModelEntity modelEntity = model.getEntityByName(entityName);
        if (modelEntity != null) {
            ModelAttrib attribute = modelEntity.getModelAttributeByName(attributeName);
            if (attribute != null) {
               return data.get(attribute.getId());
            }
        }
        return null;
    }
    
    public static void setAttributeValue(Model model, EntityData data, String entityName, String attributeName, Object value) {
        ModelEntity modelEntity = model.getEntityByName(entityName);
        if (modelEntity != null) {
            ModelAttrib attribute = modelEntity.getModelAttributeByName(attributeName);
            if (attribute != null) {
                data.put(attribute.getId(), value);
            }
        }
    }    
    
    public static boolean containsEntity(Model model, EntityData data, String entityName) {
        ModelEntity modelEntity = model.getEntityByName(entityName);
        if (modelEntity != null) {
            List<ModelAttrib> attributes = modelEntity.getModelAttributes();
            for (ModelAttrib modelAttribute : attributes) {
                if (data.containsKey(modelAttribute.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Object getAttributeValue(Model model, EntityData data, String attributeName) {
        List<ModelEntity> entites = model.getModelEntities();
        for (ModelEntity modelEntity : entites) {
            ModelAttrib attribute = modelEntity.getModelAttributeByName(attributeName);
            if (attribute != null) {
               return data.get(attribute.getId());
            }
        }
        return null;
    }

    public static List<Object> getAttributeValues(Model model, List<EntityData> rows, String entityName, String attributeName) {
        List<Object> values = new ArrayList<Object>();
        if (model != null && rows != null) {
            ModelAttrib attribute = model.getAttributeByName(entityName, attributeName);
            if (attribute != null) {
                for (EntityData data : rows) {
                    if (data.containsKey(attribute.getId())) {
                        values.add(data.get(attribute.getId()));
                    }
                }
            }
        }
        return values;
    }
}
