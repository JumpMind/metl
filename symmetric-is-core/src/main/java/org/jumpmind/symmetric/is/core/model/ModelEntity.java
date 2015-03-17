package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class ModelEntity extends AbstractObject {

    private static final long serialVersionUID = 1L;
 
    Map<String, ModelAttribute> modelAttributes;
    
    List<ModelEntityRelationship> modelEntityRelationships;
    
    String modelVersionId;
    
    String name;
    
    public ModelEntity() {
    	modelAttributes = new LinkedCaseInsensitiveMap<ModelAttribute>();
    	modelEntityRelationships = new ArrayList<ModelEntityRelationship>();
    }
    
    public ModelEntity(String Id, String name) {
        this();
        this.id = Id;
        this.name = name;
    }
    
    public Map<String, ModelAttribute> getModelAttributes() {
		return modelAttributes;
	}


	public void setModelAttributes(Map<String, ModelAttribute> modelAttributes) {
		this.modelAttributes = modelAttributes;
	}


	public List<ModelEntityRelationship> getModelEntityRelationships() {
		return modelEntityRelationships;
	}


	public void setModelEntityRelationships(
			List<ModelEntityRelationship> modelEntityRelationships) {
		this.modelEntityRelationships = modelEntityRelationships;
	}


	public String getModelVersionId() {
		return modelVersionId;
	}


	public void setModelVersionId(String modelVersionId) {
		this.modelVersionId = modelVersionId;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getAttributeIdByName(String attributeName) {
    	for (ModelAttribute attribute:modelAttributes.values()) {
    	    if (attribute.getName().equalsIgnoreCase(attributeName)) {
    	        return attribute.getId();
    	    }
    	}
    	return null;
    }
}
