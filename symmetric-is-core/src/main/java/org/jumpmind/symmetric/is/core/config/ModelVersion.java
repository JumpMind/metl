package org.jumpmind.symmetric.is.core.config;

import java.util.Map;

import org.jumpmind.symmetric.is.core.config.data.ModelVersionData;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class ModelVersion extends AbstractObject<ModelVersionData> {

    private static final long serialVersionUID = 1L;

    Model model;
    
    Map<String, ModelEntity> modelEntities;
    
    public ModelVersion(Model model, ModelVersionData data) {
        super(data);
        this.model = model;
        data.setModelId(model.getData().getId());
    }

    public String getVersionName() {
        return data.getVersionName();
    }
    
    public void setVersionName(String versionName) {
        this.data.setVersionName(versionName);
    }
    
    public void setModel(Model model) {
        this.model = model;
        this.data.setModelId(model.getId());
    }
    
    public Model getModel() {
        return model;
    }
    
    public Map<String, ModelEntity> getModelEntities() {
    	if (modelEntities == null) {
    		modelEntities = new LinkedCaseInsensitiveMap<ModelEntity>();
    	}
    	return modelEntities;
    }
    
    public void setName(String name) {
    }
    
    public String getName() {
        return this.model.getName() + " " + data.getVersionName();
    }
    
    public boolean entityAttributeExists(String entityName, String attributeName) {
    	ModelEntity entity = modelEntities.get(entityName);
    	if (entity != null) {
    		return entity.attributeExists(attributeName);
    	}
    	return false;
    }
       
}
