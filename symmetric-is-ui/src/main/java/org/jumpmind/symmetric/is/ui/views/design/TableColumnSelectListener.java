package org.jumpmind.symmetric.is.ui.views.design;

import java.util.Collection;

import org.jumpmind.symmetric.is.core.model.ModelEntity;

public interface TableColumnSelectListener {

	public void selected(Collection<ModelEntity> modelEntityCollection);
	
}
