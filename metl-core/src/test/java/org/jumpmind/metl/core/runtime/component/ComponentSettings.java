package org.jumpmind.metl.core.runtime.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ComponentSettings {
	Map<String, String> flowParametersAsString;
	Map<String, Serializable> flowParameters;
	
	public ComponentSettings() {
		this.flowParametersAsString = new HashMap<String, String>();
		this.flowParameters = new HashMap<String, Serializable>();
	}
	
	Map<String, String> getFlowParametersAsString() {
		return flowParametersAsString;
	}

	void setFlowParametersAsString(Map<String, String> flowParametersAsString) {
		this.flowParametersAsString = flowParametersAsString;
	}

	Map<String, Serializable> getFlowParameters() {
		return flowParameters;
	}

	void setFlowParameters(Map<String, Serializable> flowParameters) {
		this.flowParameters = flowParameters;
	}
	
	
	
	
	
	
}
