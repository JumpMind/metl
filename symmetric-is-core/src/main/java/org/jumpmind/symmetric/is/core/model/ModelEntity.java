package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.List;

public class ModelEntity extends AbstractObject {

    private static final long serialVersionUID = 1L;
 
    List<ModelAttribute> modelAttributes;
    
    List<ModelEntityRelationship> modelEntityRelationships;
    
    String modelId;
    
    String name;
    
    public ModelEntity() {
    	modelAttributes = new ArrayList<ModelAttribute>();
    	modelEntityRelationships = new ArrayList<ModelEntityRelationship>();
    }
    
    public ModelEntity(String Id, String name) {
    	this();
        this.id = Id;
        this.name = name;
    }
    
    public List<ModelAttribute> getModelAttributes() {
		return modelAttributes;
	}

	public void setModelAttributes(List<ModelAttribute> modelAttributes) {
		this.modelAttributes = modelAttributes;
	}

	public void addModelAttribute(ModelAttribute modelAttribute) {
		this.modelAttributes.add(modelAttribute);
	}

	public List<ModelEntityRelationship> getModelEntityRelationships() {
		return modelEntityRelationships;
	}

	public void setModelEntityRelationships(
			List<ModelEntityRelationship> modelEntityRelationships) {
		this.modelEntityRelationships = modelEntityRelationships;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelVersionId) {
		this.modelId = modelVersionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
