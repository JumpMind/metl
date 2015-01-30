package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ModelAttributeData;

public class ModelAttribute extends AbstractObject<ModelAttributeData> {

    private static final long serialVersionUID = 1L;

    ModelEntity modelEntity;
    
    ModelEntity typeEntity;
    
    public ModelAttribute(ModelEntity modelEntity, ModelEntity typeEntity, ModelAttributeData data) {
    	
    	super(data);
    	this.modelEntity = modelEntity;
    	this.typeEntity = typeEntity;
    	data.setEntityId(modelEntity.getId());
    	data.setTypeEntityId(modelEntity.getId());
    }

	public ModelEntity getModelEntity() {
		return modelEntity;
	}

	public void setModelEntity(ModelEntity modelEntity) {
		this.modelEntity = modelEntity;
	}

	public ModelEntity getTypeEntity() {
		return typeEntity;
	}

	public void setTypeEntity(ModelEntity typeEntity) {
		this.typeEntity = typeEntity;
	}
	
}
