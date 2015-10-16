package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.ComponentAttributeSetting;

public class ComponentAttributeSettingsBuilder {
	List<ComponentAttributeSetting> settings = new ArrayList<ComponentAttributeSetting>();
	
	public ComponentAttributeSettingsBuilder() {
	}
	
	public ComponentAttributeSettingsBuilder withSetting(
			String attributeId, String componentId, String key, String value) {
		
		ComponentAttributeSetting s = new ComponentAttributeSetting(
				attributeId, componentId, key, value);
		settings.add(s);
		return this;
	}
	
	public List<ComponentAttributeSetting> build() {
		return this.settings;
	}
}
