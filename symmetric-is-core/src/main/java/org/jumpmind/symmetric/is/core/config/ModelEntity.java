package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelEntityData;

public class ModelEntity extends AbstractObject<ModelEntityData> {

    private static final long serialVersionUID = 1L;

    ModelVersion modelVersion;
    
    List<ModelAttribute> modelAttributes;
    
    public ModelEntity(ModelVersion modelVersion, ModelEntityData data) {
    	super(data);
    	this.modelVersion = modelVersion;
    	data.setModelVersionId(modelVersion.getId());
    }
    
    public ModelVersion getModelVersion() {
    	return modelVersion;
    }
    
    public void setModelVersion(ModelVersion modelVersion) {
    	this.modelVersion = modelVersion;
    }
    
    public String getName() {
    	return data.getName();
    }
    
    public void setName(String name) {
    	data.setName(name);
    }

    public List<ModelAttribute> getModelAttributes() {
    	return modelAttributes;
    }
}
