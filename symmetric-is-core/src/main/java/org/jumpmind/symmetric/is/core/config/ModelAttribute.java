package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ModelAttributeData;

public class ModelAttribute extends AbstractObject<ModelAttributeData> {

    private static final long serialVersionUID = 1L;

    ModelEntity typeEntity;
    
    public ModelAttribute(ModelEntity modelEntity, ModelEntity typeEntity, ModelAttributeData data) {
    	
    	super(data);
    	this.typeEntity = typeEntity;
    	data.setEntityId(modelEntity.getId());
    	data.setTypeEntityId(modelEntity.getId());
    }

	public ModelEntity getTypeEntity() {
		return typeEntity;
	}

	public void setTypeEntity(ModelEntity typeEntity) {
		this.typeEntity = typeEntity;
	}
	
    public void setName(String name) {
        this.data.setName(name);
    }
    
    public String getName() {
        return this.data.getName();
    }
}
