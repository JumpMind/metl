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
import java.util.Set;
import java.util.UUID;

import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.runtime.EntityData;

public class Component extends AbstractObjectWithSettings {

    private static final long serialVersionUID = 1L;

    String type;

    boolean shared;

    String name;

    Resource resource;

    Model inputModel;

    Model outputModel;

    String projectVersionId;

    boolean deleted = false;

    String folderId;

    String rowId = UUID.randomUUID().toString();

    List<ComponentEntitySetting> entitySettings;

    List<ComponentAttributeSetting> attributeSettings;

    public Component() {
    }

    public Component(String id) {
        this();
        this.id = id;
    }

    public Component(Resource resource, Model inputModel, Model outputModel,
            List<ComponentEntitySetting> entitySettings,
            List<ComponentAttributeSetting> attributeSettings, Setting... settings) {
        super(settings);
        this.resource = resource;
        this.inputModel = inputModel;
        this.outputModel = outputModel;
        this.entitySettings = entitySettings;
        this.attributeSettings = attributeSettings;
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

    public void setInputModel(Model inputModel) {
        this.inputModel = inputModel;
    }

    public void setOutputModel(Model outputModel) {
        this.outputModel = outputModel;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public String getInputModelId() {
        return inputModel != null ? inputModel.getId() : null;
    }

    public void setInputModelId(String inputModeldId) {
        if (inputModeldId != null) {
            this.inputModel = new Model(inputModeldId);
        } else {
            this.inputModel = null;
        }
    }

    public String getOutputModelId() {
        return outputModel != null ? outputModel.getId() : null;
    }

    public void setOutputModelId(String outputModelId) {
        if (outputModelId != null) {
            this.outputModel = new Model(outputModelId);
        } else {
            this.outputModel = null;
        }
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

    public Model getInputModel() {
        return inputModel;
    }

    public Model getOutputModel() {
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

    public List<ComponentAttributeSetting> getAttributeSettings() {
        return attributeSettings;
    }

    public List<ComponentAttributeSetting> getAttributeSettingsFor(String entityId) {
        List<ComponentAttributeSetting> settings = new ArrayList<ComponentAttributeSetting>();
        for (ComponentAttributeSetting setting : attributeSettings) {
            String attributeId = setting.getAttributeId();
            if (inputModel != null) {
                ModelAttribute attribute = inputModel.getAttributeById(attributeId);
                if (attribute != null && attribute.getEntityId().equals(entityId)) {
                    settings.add(setting);
                }
            }

            if (outputModel != null) {
                ModelAttribute attribute = outputModel.getAttributeById(attributeId);
                if (attribute != null && attribute.getEntityId().equals(entityId)
                        && !settings.contains(setting)) {
                    settings.add(setting);
                }
            }
        }
        return settings;
    }

    public void setAttributeSettings(List<ComponentAttributeSetting> attributeSettings) {
        this.attributeSettings = attributeSettings;
    }

    public void addAttributeSetting(ComponentAttributeSetting attributeSetting) {
        if (attributeSettings == null) {
            attributeSettings = new ArrayList<ComponentAttributeSetting>();
        }
        attributeSettings.add(attributeSetting);
    }

    public ComponentAttributeSetting getSingleAttributeSetting(String attributeId, String name) {
        List<ComponentAttributeSetting> list = getAttributeSetting(attributeId, name);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public List<ComponentAttributeSetting> getAttributeSetting(String attributeId, String name) {
        List<ComponentAttributeSetting> list = new ArrayList<ComponentAttributeSetting>();
        for (ComponentAttributeSetting setting : attributeSettings) {
            if (setting.getAttributeId().equals(attributeId)
                    && setting.getName().equalsIgnoreCase(name)) {
                list.add(setting);
            }
        }
        return list;
    }

    @Override
    protected Setting createSettingData() {
        return new ComponentSetting(id);
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

	public Row toRow(EntityData data, boolean qualifyWithEntityName) {
        Row row = new Row(data.size());
        Set<String> attributeIds = data.keySet();
        for (String attributeId : attributeIds) {
            ModelAttribute attribute = inputModel.getAttributeById(attributeId);
            if (attribute != null) {
                ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                if (qualifyWithEntityName) {
                    row.put(entity.getName() + "." + attribute.getName(), data.get(attributeId));
                } else {
                    row.put(attribute.getName(), data.get(attributeId));
                }
            }
        }
        return row;
    }

    @Override
    public AbstractObject copy() {
        Component component = (Component) super.copy();
        component.setEntitySettings(new ArrayList<ComponentEntitySetting>());
        component.setAttributeSettings(new ArrayList<ComponentAttributeSetting>());
        component.setSettings(new ArrayList<Setting>());

        for (Setting setting : settings) {
            ComponentSetting cSetting = (ComponentSetting) setting.copy();
            cSetting.setComponentId(component.getId());
            component.getSettings().add(cSetting);
        }

        for (ComponentAttributeSetting setting : attributeSettings) {
            setting = (ComponentAttributeSetting) setting.copy();
            setting.setComponentId(component.getId());
            component.getAttributeSettings().add(setting);
        }

        for (ComponentEntitySetting setting : entitySettings) {
            setting = (ComponentEntitySetting) setting.copy();
            setting.setComponentId(component.getId());
            component.getEntitySettings().add(setting);
        }

        return component;
    }
}
