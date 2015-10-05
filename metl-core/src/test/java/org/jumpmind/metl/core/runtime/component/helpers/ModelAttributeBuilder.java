package org.jumpmind.metl.core.runtime.component.helpers;

import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.ModelAttribute;

public class ModelAttributeBuilder {
	ModelAttribute attribute = new ModelAttribute();
	
	public ModelAttributeBuilder() {
		
	}
	
	public ModelAttributeBuilder withEntityId(String entityId) {
		attribute.setEntityId(entityId);
		return this;
	}
	
	public ModelAttributeBuilder withId(String id) {
		attribute.setId(id);
		return this;
	}
	public ModelAttributeBuilder withName(String name) {
		attribute.setName(name);
		return this;
	}
	
	public ModelAttributeBuilder withTypeEntityId(String typeEntityId) {
		attribute.setTypeEntityId(typeEntityId);
		return this;
	}
	
	public ModelAttributeBuilder withType(String type) {
		attribute.setType(type);
		return this;
	}
	
	public ModelAttribute build() {
		return attribute;
	}
}
