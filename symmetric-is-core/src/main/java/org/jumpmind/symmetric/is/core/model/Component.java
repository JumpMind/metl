package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jumpmind.db.sql.Row;
import org.jumpmind.symmetric.is.core.runtime.EntityData;

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
            if (setting.getEntityId().equals(entityId)
                    && setting.getName().equalsIgnoreCase(name)) {
                list.add(setting);
            }
        }
        return list;
    }    
    
    public List<ComponentAttributeSetting> getAttributeSettings() {
        return attributeSettings;
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
    
    public Row toRow(EntityData data) {
        Row row = new Row(data.size());
        Set<String> attributeIds = data.keySet();
        for (String attributeId : attributeIds) {
            ModelAttribute attribute = inputModel.getAttributeById(attributeId);
            if (attribute != null) {
                row.put(attribute.getName(), data.get(attributeId));
            }
        }
        return row;
    }
}
