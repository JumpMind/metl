package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

abstract public class AbstractComponent extends AbstractRuntimeObject implements IComponent {

    @SettingDefinition(order = 100, required = false, type = Type.INTEGER, defaultValue = "10000", label = "Inbound Queue Capacity")
    public final static String INBOUND_QUEUE_CAPACITY = "inbound.queue.capacity";
    
    protected Flow flow;
    protected FlowStep flowStep;    
    protected IResource resource;
    protected IResourceFactory resourceFactory;
    protected IExecutionTracker executionTracker;
    protected ComponentStatistics componentStatistics;

    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        this.componentStatistics = new ComponentStatistics();
    	this.executionTracker = executionTracker;
        this.resourceFactory = resourceFactory;

        Resource resource = flowStep.getComponent().getResource();
        if (resource != null) {
            try {
                this.resource = resourceFactory.create(resource);
                this.resource.start(resource);
                //TODO: resource.start gets called twice here...  Once during the create and again in the start after it.
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        if (this.resource != null) {
            this.resource.stop();
        }
    }
    
    public ComponentStatistics getComponentStatistics() {
    	return componentStatistics;
    }
    
    public FlowStep getFlowStep() {
    	return this.flowStep;
    }
    
    public void init(FlowStep flowStep, Flow flow) {
    	this.flowStep = flowStep;
    	this.flow = flow;
    }
    
    @Override
    public void flowCompletedWithoutError() {
    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors) {
    }
    
}
