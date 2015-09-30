package org.jumpmind.metl.core.runtime.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.FlowStep;

public class ComponentSettings {
	Map<String, String> flowParametersAsString;
	Map<String, Serializable> flowParameters;
	FlowStep flowStep;
	
	public ComponentSettings() {
		this.flowParametersAsString = new HashMap<String, String>();
		this.flowParameters = new HashMap<String, Serializable>();
		this.flowStep = new FlowStep();
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

	FlowStep getFlowStep() {
		return flowStep;
	}

	void setFlowStep(FlowStep flowStep) {
		this.flowStep = flowStep;
	}
	
	
	
	
	
	
}
