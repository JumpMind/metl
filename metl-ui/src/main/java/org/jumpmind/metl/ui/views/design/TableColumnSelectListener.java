package org.jumpmind.metl.ui.views.design;

import java.util.Collection;

import org.jumpmind.metl.core.model.ModelEntity;

public interface TableColumnSelectListener {

	public void selected(Collection<ModelEntity> modelEntityCollection);
	
}
