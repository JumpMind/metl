package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelEntityRelationshipData;

public class ModelEntityRelationship extends AbstractObject<ModelEntityRelationshipData> {

    private static final long serialVersionUID = 1L;

    List<ModelAttributeRelationship> attributeRelationships;
 
    public ModelEntityRelationship(ModelEntityRelationshipData data) {
    	
    	super(data);
    }
    
    public String getName() {
    	return data.getName();
    }
    
	public List<ModelAttributeRelationship> getAttributeRelationships() {
		return attributeRelationships;
	}
    
}
