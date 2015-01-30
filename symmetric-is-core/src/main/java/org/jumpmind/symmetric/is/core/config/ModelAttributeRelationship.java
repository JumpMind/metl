package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ModelAttributeRelationshipData;

public class ModelAttributeRelationship extends AbstractObject<ModelAttributeRelationshipData> {
	
    private static final long serialVersionUID = 1L;
    
    ModelEntityRelationship entityRelationship;
    
    ModelAttribute sourceAttribute;
    
    ModelAttribute targetAttribute;
    
    public ModelAttributeRelationship(ModelEntityRelationship entityRelationship,
    		ModelAttribute sourceAttribute, ModelAttribute targetAttribute,
    		ModelAttributeRelationshipData data) {
    	
    	super(data);
    	this.entityRelationship = entityRelationship;
    	this.sourceAttribute = sourceAttribute;
    	this.targetAttribute = targetAttribute;
    	data.setSourceAttributeId(sourceAttribute.getId());
    	data.setTargetAttributeId(targetAttribute.getId());
    	data.setEntityRelationshipId(entityRelationship.getId());
    }

	public ModelEntityRelationship getEntityRelationship() {
		return entityRelationship;
	}

	public void setEntityRelationship(ModelEntityRelationship entityRelationship) {
		this.entityRelationship = entityRelationship;
	}

	public ModelAttribute getSourceAttribute() {
		return sourceAttribute;
	}

	public void setSourceAttribute(ModelAttribute sourceAttribute) {
		this.sourceAttribute = sourceAttribute;
	}

	public ModelAttribute getTargetAttribute() {
		return targetAttribute;
	}

	public void setTargetAttribute(ModelAttribute targetAttribute) {
		this.targetAttribute = targetAttribute;
	}
    
}
