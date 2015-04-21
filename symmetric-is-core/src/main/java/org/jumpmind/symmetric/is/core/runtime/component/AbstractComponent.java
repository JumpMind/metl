package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;

abstract public class AbstractComponent extends AbstractRuntimeObject implements IComponent {

    @SettingDefinition(order = 100, required = false, type = Type.INTEGER, defaultValue = "10000", label = "Inbound Queue Capacity")
    public final static String INBOUND_QUEUE_CAPACITY = "inbound.queue.capacity";
    
    protected Flow flow;
    protected FlowStep flowStep; 
    protected Map<String, IResource> resources;
    protected IResource resource;
    protected IExecutionTracker executionTracker;
    protected ComponentStatistics componentStatistics;

    public void start(IExecutionTracker executionTracker) {
        this.componentStatistics = new ComponentStatistics();
    	this.executionTracker = executionTracker;
    }

    public void stop() {
    }
    
    public ComponentStatistics getComponentStatistics() {
    	return componentStatistics;
    }
    
    public FlowStep getFlowStep() {
    	return this.flowStep;
    }
    
    public void init(FlowStep flowStep, Flow flow, Map<String, IResource> resources) {
    	this.flowStep = flowStep;
    	this.flow = flow;
    	this.resources = resources;
    	Resource r = flowStep.getComponent().getResource();
    	if (r != null) {
    	    resource = resources.get(r.getId());
    	}
    }
    
    @Override
    public void flowCompletedWithoutError() {
    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors) {
    }
    
}
