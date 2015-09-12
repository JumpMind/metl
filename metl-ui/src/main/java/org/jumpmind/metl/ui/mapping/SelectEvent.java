package org.jumpmind.metl.ui.mapping;

import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Event;

public class SelectEvent extends Event {

	private static final long serialVersionUID = 1L;

	String selectedSourceId;
	
	String selectedTargetId;
	
	public SelectEvent(Component component, String selectSourceId, String selectedTargetId) {
		super(component);
		this.selectedSourceId = selectSourceId;
		this.selectedTargetId = selectedTargetId;
	}

	public String getSelectedSourceId() {
		return selectedSourceId;
	}

	public String getSelectedTargetId() {
		return selectedTargetId;
	}

}
