package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;

import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelEntity;

public class ModelBuilder {
	Model model;
	
	public ModelBuilder() {
		model = new Model();
		model.setModelEntities(new ArrayList<ModelEntity>());
	}
	
	public ModelBuilder(Model m) {
		model = m;
		if (model.getModelEntities() == null) {
			model.setModelEntities(new ArrayList<ModelEntity>());
		}
	}
	public ModelBuilder withFolder(Folder folder) {
		model.setFolder(folder);
		return this;
	}
	
	public ModelBuilder withId(String id) {
		model.setId(id);
		return this;
	}

	public ModelBuilder withName(String name) {
		model.setName(name);
		return this;
	}
	
	public ModelBuilder withProjectVersionId(String id) {
		model.setProjectVersionId(id);
		return this;
	}
	
	public ModelBuilder withShared(boolean shared) {
		model.setShared(shared);
		return this;
	}
	
	public ModelBuilder withDeleted(boolean deleted) {
		model.setDeleted(deleted);
		return this;
	}
	
	public ModelBuilder withEntity(ModelEntity entity) {
		this.model.getModelEntities().add(entity);
		return this;
	}
	
	public Model build() {
		return this.model;
	}
	
	
	
}
