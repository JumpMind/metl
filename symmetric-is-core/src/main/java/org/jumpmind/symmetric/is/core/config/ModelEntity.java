package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelEntityData;

public class ModelEntity extends AbstractObject<ModelEntityData> {

    private static final long serialVersionUID = 1L;
 
    List<ModelAttribute> modelAttributes;
    
    List<ModelEntityRelationship> modelEntityRelationships;
    
    public ModelEntity(ModelVersion modelVersion, ModelEntityData data) {
    	super(data);
    	data.setModelVersionId(modelVersion.getId());
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
    
    public List<ModelEntityRelationship> getModelEntityRelationships() {
    	return modelEntityRelationships;
    }
}
