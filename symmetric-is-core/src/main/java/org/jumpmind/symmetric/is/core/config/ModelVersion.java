package org.jumpmind.symmetric.is.core.config;

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
    
    public ModelVersion(Model model) {
    	this();
    	this.model = model;
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

	public boolean entityAttributeExists(String entityName, String attributeName) {
    	ModelEntity entity = modelEntities.get(entityName);
    	if (entity != null) {
    		return entity.attributeExists(attributeName);
    	}
    	return false;
    }

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
       
}
