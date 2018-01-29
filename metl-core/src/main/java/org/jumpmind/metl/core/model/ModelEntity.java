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

public class ModelEntity extends AbstractNamedObject implements IAuditable, Comparable<ModelEntity> {

    private static final long serialVersionUID = 1L;

    List<ModelAttrib> modelAttributes;

    String modelId;

    String name;
    
    String description;

    public ModelEntity() {
        modelAttributes = new ArrayList<ModelAttrib>();
    }

    public ModelEntity(String id, String name) {
        this();
        setId(id);
        this.name = name;
    }

    public List<ModelAttrib> getModelAttributes() {
        return modelAttributes;
    }

    public void setModelAttributes(List<ModelAttrib> modelAttributes) {
        this.modelAttributes = modelAttributes;
    }

    public void addModelAttribute(ModelAttrib modelAttribute) {
        modelAttribute.setAttributeOrder(modelAttributes.size());
        this.modelAttributes.add(modelAttribute);
    }

    public void removeModelAttribute(ModelAttrib modelAttribute) {
        this.modelAttributes.remove(modelAttribute);
    }

    public ModelAttrib getModelAttributeByName(String name) {
        for (ModelAttrib modelAttribute : modelAttributes) {
            if (modelAttribute.getName().equalsIgnoreCase(name)) {
                return modelAttribute;
            }
        }
        return null;
    }

    public ModelAttrib getModelAttributeByTypeEntityId(String typeEntityId) {
        for (ModelAttrib modelAttribute : modelAttributes) {
            if (modelAttribute.getTypeEntityId() != null && modelAttribute.getTypeEntityId().equalsIgnoreCase(typeEntityId)) {
                return modelAttribute;
            }
        }
        return null;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelVersionId) {
        this.modelId = modelVersionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean hasOnlyPrimaryKeys() {
        boolean pksOnly = true;
        for (ModelAttrib modelAttribute : modelAttributes) {
            pksOnly &= modelAttribute.isPk();
        }
        return pksOnly;
    }

	@Override
	public int compareTo(ModelEntity o) {
		for (ModelAttrib attrib:this.getModelAttributes()) {
			if (attrib.getTypeEntityId() != null && 
					attrib.getTypeEntityId().equalsIgnoreCase(o.getId())) {
				return -1;
			}
		}
		return 1;
	}
}
