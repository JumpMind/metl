package org.jumpmind.metl.core.runtime.component.helpers;

import org.jumpmind.metl.core.runtime.EntityData;

public class EntityDataBuilder {
	EntityData entityData = new EntityData();
	
	public EntityDataBuilder() {
	}
	
	public EntityDataBuilder addKV(String key, Object value) {
		entityData.put(key, value);
		return this;
	}
	
	public EntityData build() {
		return this.entityData;
	}
	
	
}
