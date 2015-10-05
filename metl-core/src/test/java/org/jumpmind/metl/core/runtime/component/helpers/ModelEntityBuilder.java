package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;

import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;

public class ModelEntityBuilder {
	ModelEntity entity;
	
	public ModelEntityBuilder() {
		entity = new ModelEntity();
		entity.setModelAttributes(new ArrayList<ModelAttribute>());
	}
	
	public ModelEntityBuilder withId(String id) {
		entity.setId(id);
		return this;
	}
	
	public ModelEntityBuilder withName(String name) {
		entity.setName(name);
		return this;
	}
	
	public ModelEntityBuilder withModelId(String modelId) {
		entity.setModelId(modelId);
		return this;
	}
	
	public ModelEntityBuilder withAttribute(ModelAttribute attr) {
		entity.addModelAttribute(attr);
		return this;
	}
	
	public ModelEntity build() {
		return this.entity;
	}
}
