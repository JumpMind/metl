package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceRuntime;

public class ComponentContext {

    FlowStep flowStep;

    Flow flow;

    IExecutionTracker executionTracker;

    IResourceRuntime resourceRuntime;
    
    Map<String, Serializable> flowParameters;
    
    Map<String, String> globalSettings;
    
    ComponentStatistics componentStatistics = new ComponentStatistics();

    public ComponentContext(FlowStep flowStep, Flow flow, IExecutionTracker executionTracker,
            IResourceRuntime resource, Map<String, Serializable> flowParameters, Map<String, String> globalSettings) {
        if (flowParameters == null) {
            flowParameters = new HashMap<String, Serializable>();
        }
        this.flowStep = flowStep;
        this.flow = flow;
        this.executionTracker = executionTracker;
        this.resourceRuntime = resource;
        this.flowParameters = flowParameters;
        this.globalSettings = globalSettings;
    }

    public FlowStep getFlowStep() {
        return flowStep;
    }

    public Flow getFlow() {
        return flow;
    }

    public IExecutionTracker getExecutionTracker() {
        return executionTracker;
    }

    public IResourceRuntime getResourceRuntime() {
        return resourceRuntime;
    }

    public Map<String, Serializable> getFlowParameters() {
        return flowParameters;
    }
    
    public Map<String, String> getFlowParametersAsString() {
        Map<String, String> params = new HashMap<String, String>();
        for (String key : flowParameters.keySet()) {
            Serializable value = flowParameters.get(key);
            if (value != null) {
                params.put(key, value.toString());
            } else {
                params.put(key, null);
            }
        }
        return params;
    }
    
    public void setComponentStatistics(ComponentStatistics componentStatistics) {
        this.componentStatistics = componentStatistics;
    }
    
    public ComponentStatistics getComponentStatistics() {
        return componentStatistics;
    }

    public Map<String, String> getGlobalSettings() {
        return globalSettings;
    }

}
