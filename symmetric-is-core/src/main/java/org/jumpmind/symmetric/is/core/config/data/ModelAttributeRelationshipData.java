package org.jumpmind.symmetric.is.core.config.data;

public class ModelAttributeRelationshipData extends AbstractData {

    private static final long serialVersionUID = 1L;
    
    String entityRelationshipId;
    
    String sourceAttributeId;
    
    String targetAttributeId;

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
    
}
