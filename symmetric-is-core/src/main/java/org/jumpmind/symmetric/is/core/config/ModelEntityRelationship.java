package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelEntityRelationshipData;

public class ModelEntityRelationship extends AbstractObject<ModelEntityRelationshipData> {

    private static final long serialVersionUID = 1L;

    ModelEntity sourceEntity;
    
    ModelEntity targetEntity;
    
    List<ModelAttributeRelationship> attributeRelationships;
    
    public ModelEntityRelationship(ModelEntity sourceEntity, ModelEntity targetEntity, ModelEntityRelationshipData data) {
    	
    	super(data);
    	this.sourceEntity = sourceEntity;
    	data.setSourceEntityId(sourceEntity.getId());
    	this.targetEntity = targetEntity;
    	data.setTargetEntityId(targetEntity.getId());
    }

    public String getName() {
    	return data.getName();
    }
    
	public ModelEntity getSourceEntity() {
		return sourceEntity;
	}

	public void setSourceEntity(ModelEntity sourceEntity) {
		this.sourceEntity = sourceEntity;
	}

	public ModelEntity getTargetEntity() {
		return targetEntity;
	}

	public void setTargetEntity(ModelEntity targetEntity) {
		this.targetEntity = targetEntity;
	}
    
	public List<ModelAttributeRelationship> getAttributeRelationships() {
		return attributeRelationships;
	}
    
}
