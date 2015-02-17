package org.jumpmind.symmetric.is.core.config;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.data.ModelEntityData;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class ModelEntity extends AbstractObject<ModelEntityData> {

    private static final long serialVersionUID = 1L;
 
    Map<String, ModelAttribute> modelAttributes;
    
    List<ModelEntityRelationship> modelEntityRelationships;
    
    public ModelEntity(ModelVersion modelVersion, ModelEntityData data) {
    	super(data);
    	data.setModelVersionId(modelVersion.getId());
    }
    
    public String getName() {
    	return data.getName();
    }
    
    public void setName(String name) {
    	data.setName(name);
    }

    public Map<String, ModelAttribute> getModelAttributes() {
    	if (modelAttributes == null) {
    		modelAttributes = new LinkedCaseInsensitiveMap<ModelAttribute>();
    	}
    	return modelAttributes;
    }
    
    public List<ModelEntityRelationship> getModelEntityRelationships() {
    	return modelEntityRelationships;
    }
    
    public boolean attributeExists(String attributeName) {
    	return modelAttributes.containsKey(attributeName);
    }
}
