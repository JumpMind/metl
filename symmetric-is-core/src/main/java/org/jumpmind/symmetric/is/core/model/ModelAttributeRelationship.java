package org.jumpmind.symmetric.is.core.model;

public class ModelAttributeRelationship extends AbstractObject {
	
    private static final long serialVersionUID = 1L;
    
    ModelAttribute sourceAttribute;
    
    ModelAttribute targetAttribute;
    
    String entityRelationshipId;
    
    String sourceAttributeId;
    
    String targetAttributeId;

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

	public String getEntityRelationshipId() {
		return entityRelationshipId;
	}

	public void setEntityRelationshipId(String entityRelationshipId) {
		this.entityRelationshipId = entityRelationshipId;
	}

	public String getSourceAttributeId() {
		return sourceAttributeId;
	}

	public void setSourceAttributeId(String sourceAttributeId) {
		this.sourceAttributeId = sourceAttributeId;
	}

	public String getTargetAttributeId() {
		return targetAttributeId;
	}

	public void setTargetAttributeId(String targetAttributeId) {
		this.targetAttributeId = targetAttributeId;
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
