package org.jumpmind.symmetric.is.core.model;

import java.util.Map;

import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class Model extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Folder folder;

    String name;

    String type = "NORMAL";

    String folderId;

    Map<String, ModelEntity> modelEntities;

    boolean shared;

    public Model() {
        this.modelEntities = new LinkedCaseInsensitiveMap<ModelEntity>();
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public ModelEntity getEntityByName(String entityName) {
        for (ModelEntity entity : modelEntities.values()) {
            if (entity.getName().equalsIgnoreCase(entityName)) {
                return entity;
            }
        }
        return null;
    }

    public ModelAttribute getAttributeByName(String entityName, String attributeName) {
        ModelEntity entity = getEntityByName(entityName);
        if (entity != null) {
            for (ModelAttribute modelAttribute : entity.getModelAttributes()) {
                if (modelAttribute.getName().equalsIgnoreCase(attributeName)) {
                    return modelAttribute;
                }
            }
        }
        return null;
    }
    
    public Map<String, ModelEntity> getModelEntities() {
        return modelEntities;
    }

}
