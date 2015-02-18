package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelEntityRelationshipData;

public class ModelEntityRelationship extends DeprecatedAbstractObject<ModelEntityRelationshipData> {

    private static final long serialVersionUID = 1L;

    List<ModelAttributeRelationship> attributeRelationships;
 
    public ModelEntityRelationship(ModelEntityRelationshipData data) {
    	
    	super(data);
    }
    
	public List<ModelAttributeRelationship> getAttributeRelationships() {
		return attributeRelationships;
	}
	
    public void setName(String name) {
        this.data.setName(name);
    }
    
    public String getName() {
        return this.data.getName();
    }
    
}
