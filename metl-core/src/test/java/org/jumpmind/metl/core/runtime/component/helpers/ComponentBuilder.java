package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;

public class ComponentBuilder {
	Component component = new Component();
	
	public ComponentBuilder() {
	}
	
	public ComponentBuilder withAttributeSettings(List<ComponentAttributeSetting> settings) {
		component.setAttributeSettings(settings);
		return this;
	}
	
	public ComponentBuilder withSettings(List<Setting> settings) {
		component.setSettings(settings);
		return this;
	}
	
	public ComponentBuilder withResource(Resource resource) {
		component.setResource(resource);
		return this;
	}
	public Component build() {
		return this.component;
	}
}
