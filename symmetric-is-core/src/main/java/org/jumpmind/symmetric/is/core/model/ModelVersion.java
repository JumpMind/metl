package org.jumpmind.symmetric.is.core.model;

import java.util.Map;
import java.util.UUID;

import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class ModelVersion extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Model model;
    
    Map<String, ModelEntity> modelEntities;

    String versionName = UUID.randomUUID().toString();
    
    String modelId;

    public ModelVersion() {
    	this.modelEntities = new LinkedCaseInsensitiveMap<ModelEntity>();
    }

    public ModelVersion(String id) {
    	this();
    	this.id = id;
    }
    
    public ModelVersion(Model model) {
    	this();
    	this.model = model;
    	this.modelId = model.getId();
    }
    
    public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public Map<String, ModelEntity> getModelEntities() {
		return modelEntities;
	}

	public void setModelEntities(Map<String, ModelEntity> modelEntities) {
		this.modelEntities = modelEntities;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public ModelEntity getEntityByName(String entityName) {
	    for (ModelEntity entity:modelEntities.values()) {
	        if (entity.getName().equalsIgnoreCase(entityName)) {
	            return entity;
	        }
	    }
	    return null;
	}
	
	public String getAttributeId(String entityName, String attributeName) {
    	ModelEntity entity = getEntityByName(entityName);
    	if (entity != null) {
    		return entity.getAttributeIdByName(attributeName);
    	}
    	return null;
    }

	@Override
	public void setName(String name) {
		versionName = name;
	}

	@Override
	public String getName() {
		return versionName;
	}
       
}
