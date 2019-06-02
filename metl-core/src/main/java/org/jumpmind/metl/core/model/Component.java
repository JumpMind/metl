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
import java.util.UUID;

import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.util.LogUtils;

public class Component extends AbstractObjectWithSettings implements IAuditable {

    private static final long serialVersionUID = 1L;

    String type;

    boolean shared;

    String name;

    Resource resource;

    String inputModelId;
    
    IModel inputModel;

    String outputModelId;
    
    IModel outputModel;
    
    String projectVersionId;

    boolean deleted = false;

    String folderId;

    String rowId = UUID.randomUUID().toString();
    
    List<ComponentEntitySetting> entitySettings;

    List<ComponentAttribSetting> attributeSettings;
    
    List<ComponentModelSetting> modelSettings;

    public Component() {
    }

    public Component(String id) {
        this();
        setId(id);
    }

    public Component(Resource resource, IModel inputModel, IModel outputModel,
            List<ComponentEntitySetting> entitySettings,
            List<ComponentAttribSetting> attributeSettings, 
            List<ComponentModelSetting> modelSettings, Setting... settings) {
        super(settings);
        this.resource = resource;
        this.inputModel = inputModel;
        this.outputModel = outputModel;
        this.entitySettings = entitySettings;
        this.attributeSettings = attributeSettings;
        this.modelSettings = modelSettings;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setInputModel(IModel inputModel) {
        this.inputModel = inputModel;
    }

    public void setOutputModel(IModel outputModel) {
        this.outputModel = outputModel;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public String getInputModelId() {
        return inputModelId;
    }

    public void setInputModelId(String inputModelId) {
        this.inputModelId = inputModelId;
    }

    public String getOutputModelId() {
        return outputModelId;
    }
    
    public void setOutputModelId(String outputModelId) {
        this.outputModelId = outputModelId;
    }

    public String getResourceId() {
        return resource != null ? resource.getId() : null;
    }

    public void setResourceId(String resourceId) {
        if (resourceId != null) {
            this.resource = new Resource(resourceId);
        } else {
            this.resource = null;
        }
    }

    public IModel getInputModel() {
        return inputModel;
    }

    public IModel getOutputModel() {
        return outputModel;
    }

    public List<ComponentEntitySetting> getEntitySettings() {
        return entitySettings;
    }

    public void setEntitySettings(List<ComponentEntitySetting> entitySettings) {
        this.entitySettings = entitySettings;
    }

    public void addEntitySetting(ComponentEntitySetting entitySetting) {
        if (entitySettings == null) {
            entitySettings = new ArrayList<ComponentEntitySetting>();
        }
        entitySettings.add(entitySetting);
    }

    public ComponentEntitySetting getSingleEntitySetting(String entityId, String name) {
        List<ComponentEntitySetting> list = getEntitySetting(entityId, name);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public List<ComponentEntitySetting> getEntitySetting(String entityId, String name) {
        List<ComponentEntitySetting> list = new ArrayList<ComponentEntitySetting>();
        for (ComponentEntitySetting setting : entitySettings) {
            if (setting.getEntityId().equals(entityId) && setting.getName().equalsIgnoreCase(name)) {
                list.add(setting);
            }
        }
        return list;
    }

    public List<ComponentAttribSetting> getAttributeSettings() {
        return attributeSettings;
    }

    public List<ComponentAttribSetting> getAttributeSettingsFor(String entityId) {
        List<ComponentAttribSetting> settings = new ArrayList<ComponentAttribSetting>();
        for (ComponentAttribSetting setting : attributeSettings) {
            String attributeId = setting.getAttributeId();
            if (inputModel != null && inputModel instanceof RelationalModel) {
                RelationalModel inRelationalModel = (RelationalModel) inputModel;
                ModelAttrib attribute = inRelationalModel.getAttributeById(attributeId);
                if (attribute != null && attribute.getEntityId().equals(entityId)) {
                    settings.add(setting);
                }
            }

            if (outputModel != null && outputModel instanceof RelationalModel) {
                RelationalModel outRelationalModel = (RelationalModel) outputModel;
                ModelAttrib attribute = outRelationalModel.getAttributeById(attributeId);
                if (attribute != null && attribute.getEntityId().equals(entityId)
                        && !settings.contains(setting)) {
                    settings.add(setting);
                }
            }
        }
        return settings;
    }
    
    //TODO: DO SOMETHING SIMILAR HERE FOR HIERARCHICAL MODEL

    public void setAttributeSettings(List<ComponentAttribSetting> attributeSettings) {
        this.attributeSettings = attributeSettings;
    }

    public void addAttributeSetting(ComponentAttribSetting attributeSetting) {
        if (attributeSettings == null) {
            attributeSettings = new ArrayList<ComponentAttribSetting>();
        }
        attributeSettings.add(attributeSetting);
    }

    public ComponentAttribSetting getSingleAttributeSetting(String attributeId, String name) {
        List<ComponentAttribSetting> list = getAttributeSetting(attributeId, name);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public List<ComponentAttribSetting> getAttributeSetting(String attributeId, String name) {
        List<ComponentAttribSetting> list = new ArrayList<ComponentAttribSetting>();
        for (ComponentAttribSetting setting : attributeSettings) {
            if (setting.getAttributeId().equals(attributeId)
                    && setting.getName().equalsIgnoreCase(name)) {
                list.add(setting);
            }
        }
        return list;
    }

    public List<ComponentModelSetting> getModelSetting(String modelObjectId, String name) {
        List<ComponentModelSetting> list = new ArrayList<ComponentModelSetting>();
        for (ComponentModelSetting setting : modelSettings) {
            if (setting.getModelObjectId().equals(modelObjectId)
                    && setting.getName().equalsIgnoreCase(name)) {
                list.add(setting);
            }
        }
        return list;
    }

    public List<ComponentModelSetting> getModelSettings() {
        return modelSettings;
    }

    public void setModelSettings(List<ComponentModelSetting> modelSettings) {
        this.modelSettings = modelSettings;
    }

    public void addModelSetting(ComponentModelSetting modelSetting) {
        if (modelSettings == null) {
            modelSettings = new ArrayList<ComponentModelSetting>();
        }
        modelSettings.add(modelSetting);
    }    
    
    @Override
    protected Setting createSettingData() {
        return new ComponentSetting(getId());
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

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getFolderId() {
        return folderId;
    }
    
	public Row toRow(EntityData data, boolean qualifyWithEntityName, boolean input) {
        Row row = new Row(data.size()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return LogUtils.toJson(data.getChangeType().name(), this);
            }
        };
        IModel model = input ? inputModel : outputModel;
        if (model != null) {
            RelationalModel relationalModel = (RelationalModel) model;
            return relationalModel.toRow(data, qualifyWithEntityName);
        }
        return row;
    }
	
	//TODO: WILL WE NEED A TOROW FOR HIERARCHICAL MODEL
}
