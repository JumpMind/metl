package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ModelAttributeRelationshipData;

public class ModelAttributeRelationship extends AbstractObject<ModelAttributeRelationshipData> {
	
    private static final long serialVersionUID = 1L;
    
    ModelAttribute sourceAttribute;
    
    ModelAttribute targetAttribute;
    
    public ModelAttributeRelationship(
    		ModelAttributeRelationshipData data) {
    	
    	super(data);
    	data.setSourceAttributeId(sourceAttribute.getId());
    	data.setTargetAttributeId(targetAttribute.getId());
    }

    public ModelAttributeRelationship(
    		ModelAttribute sourceAttribute, ModelAttribute targetAttribute,
    		ModelAttributeRelationshipData data) {
    	
    	super(data);
    	this.sourceAttribute = sourceAttribute;
    	this.targetAttribute = targetAttribute;
    	data.setSourceAttributeId(sourceAttribute.getId());
    	data.setTargetAttributeId(targetAttribute.getId());
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
