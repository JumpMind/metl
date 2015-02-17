package org.jumpmind.symmetric.is.core.config;

import java.util.Map;

import org.jumpmind.symmetric.is.core.config.data.ModelVersionData;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class ModelVersion extends AbstractObject<ModelVersionData> {

    private static final long serialVersionUID = 1L;

    Model model;
    
    ModelFormat modelFormat;
    
    Map<String, ModelEntity> modelEntities;
    
    public ModelVersion(Model model, ModelFormat modelFormat, ModelVersionData data) {
        super(data);
        this.model = model;
        this.modelFormat = modelFormat;
        data.setModelId(model.getData().getId());
        //TODO: add model format classes
//        if (modelFormat != null) {
//            data.setModelFormatId(modelFormat.getData().getId());
//        }
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
    
    //TODO: add model format classes
//    public void setModelFormat(ModelFormat modelFormat) {
//        this.modelFormat = modelFormat;
//        this.data.setModelFormatId(modelFormat.getId());
//    }
//    
//    public ModelFormat getModelFormat() {
//        return modelFormat;
//    }
        
}
