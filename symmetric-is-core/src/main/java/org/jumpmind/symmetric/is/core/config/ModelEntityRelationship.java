package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class ModelEntityRelationship extends AbstractObject {

    private static final long serialVersionUID = 1L;

    List<ModelAttributeRelationship> attributeRelationships;
        
    ModelEntity sourceEntity;
    
    ModelEntity targetEntity;
    
    String name;
    
    String sourceEntityId;
    
    String targetEntityId;

    public ModelEntityRelationship() {
    	attributeRelationships = new ArrayList<ModelAttributeRelationship>();
    }
    
	public List<ModelAttributeRelationship> getAttributeRelationships() {
		return attributeRelationships;
	}

	public void setAttributeRelationships(
			List<ModelAttributeRelationship> attributeRelationships) {
		this.attributeRelationships = attributeRelationships;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSourceEntityId() {
		return sourceEntityId;
	}

	public void setSourceEntityId(String sourceEntityId) {
		this.sourceEntityId = sourceEntityId;
	}

	public String getTargetEntityId() {
		return targetEntityId;
	}

	public void setTargetEntityId(String targetEntityId) {
		this.targetEntityId = targetEntityId;
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
	
}
