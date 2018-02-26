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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.util.LogUtils;

public class Model extends AbstractNamedObject implements IAuditable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_RELATIONAL = "RELATIONAL";

    public static final String TYPE_HIERARCHICAL = "HIERARCHICAL";

    Folder folder;

    String name;

    String folderId;

    String projectVersionId;
    
    String type;

    String rowId = UUID.randomUUID().toString();

    List<ModelEntity> modelEntities;
    
    List<ModelRelation> modelRelations;
    
    boolean shared;

    boolean deleted = false;

    public Model() {
        this.modelEntities = new ArrayList<ModelEntity>();
        this.modelRelations = new ArrayList<ModelRelation>();
    }

    public Model(String id) {
        this();
        this.setId(id);
    }

    public Model(Folder folder) {
        this();
        this.folder = folder;
        this.folderId = folder.getId();
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ModelEntity getEntityById(String entityId) {
        for (ModelEntity entity : modelEntities) {
            if (entity.getId().equalsIgnoreCase(entityId)) {
                return entity;
            }
        }
        return null;
    }

    public ModelEntity getEntityByName(String entityName) {
        for (ModelEntity entity : modelEntities) {
            if (entity.getName().equalsIgnoreCase(entityName)) {
                return entity;
            }
        }
        return null;
    }

    public ModelAttrib getAttributeById(String attributeId) {
        for (ModelEntity entity : modelEntities) {
            for (ModelAttrib modelAttribute : entity.getModelAttributes()) {
                if (modelAttribute.getId().equalsIgnoreCase(attributeId)) {
                    return modelAttribute;
                }
            }
        }
        return null;
    }

    public ModelAttrib getAttributeByName(String entityName, String attributeName) {
        ModelEntity entity = getEntityByName(entityName);
        if (entity != null) {
            for (ModelAttrib modelAttribute : entity.getModelAttributes()) {
                if (modelAttribute.getName().equalsIgnoreCase(attributeName)) {
                    return modelAttribute;
                }
            }
        }
        return null;
    }

    public List<ModelAttrib> getAttributesByName(String attributeName) {
        List<ModelAttrib> attributes = new ArrayList<ModelAttrib>();
        for (ModelEntity entity : modelEntities) {
            for (ModelAttrib modelAttribute : entity.getModelAttributes()) {
                if (modelAttribute.getName().equalsIgnoreCase(attributeName)) {
                    attributes.add(modelAttribute);
                }
            }
        }
        return attributes;
    }

    public List<ModelEntity> getModelEntities() {
        return modelEntities;
    }

    public void setModelEntities(List<ModelEntity> modelEntities) {
        this.modelEntities = modelEntities;
    }
    
    public List<ModelRelation> getModelRelations() {
		return modelRelations;
	}

	public void setModelRelations(List<ModelRelation> modelRelations) {
		this.modelRelations = modelRelations;
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

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void sortAttributes() {
        for (ModelEntity modelEntity : modelEntities) {
            AbstractObjectNameBasedSorter.sort(modelEntity.getModelAttributes());
        }
    }

    public Row toRow(EntityData data, boolean qualifyWithEntityName) {
        Row row = new Row(data.size()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return LogUtils.toJson(data.getChangeType().name(), this);
            }
        };
        Set<String> attributeIds = data.keySet();
        for (String attributeId : attributeIds) {
            ModelAttrib attribute = getAttributeById(attributeId);
            if (attribute != null) {
                ModelEntity entity = getEntityById(attribute.getEntityId());
                if (qualifyWithEntityName) {
                    row.put(entity.getName() + "." + attribute.getName(), data.get(attributeId));
                } else {
                    row.put(attribute.getName(), data.get(attributeId));
                }
            }
        }
        return row;
    }
    
    public Map<String,String> toStringMap(EntityData data, boolean qualifyWithEntityName) {
        Map<String,String> row = new HashMap<>(data.size());
        Set<String> attributeIds = data.keySet();
        for (String attributeId : attributeIds) {
            ModelAttrib attribute = getAttributeById(attributeId);
            if (attribute != null) {
                ModelEntity entity = getEntityById(attribute.getEntityId());
                Object value = data.get(attributeId);
                if (qualifyWithEntityName) {                    
                    row.put(entity.getName() + "." + attribute.getName(), value != null ? value.toString() : null);
                } else {
                    row.put(attribute.getName(), value != null ? value.toString() : null);
                }
            }
        }
        return row;
    }
    
    public ModelEntity getRootElement() {
    	
    		if (this.type.equalsIgnoreCase(TYPE_HIERARCHICAL) && getModelEntities().size()>0) {
    			return getEntityTree().first();
    		}
    		else {
    			return null;    			
    		}
    }
    
    public TreeSet<ModelEntity> getEntityTree() {
		return new TreeSet<ModelEntity>(this.getModelEntities());
    }
    
    public ModelAttrib getModelAttribByTypeEntityId(String typeEntityId) {
        for (ModelEntity entity:this.getModelEntities()) {
            for (ModelAttrib attrib : entity.getModelAttributes()) {
                if (typeEntityId.equalsIgnoreCase(attrib.getTypeEntityId())) {
                    return attrib;
                }
            }
        }
        return null;
    }
    
}
