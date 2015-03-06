package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

abstract public class AbstractComponent extends AbstractRuntimeObject implements IComponent {

    protected FlowStep flowStep;    
    protected IResource resource;
    protected IResourceFactory resourceFactory;
    protected IExecutionTracker executionTracker;
    protected ComponentStatistics componentStatistics;

    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        this.componentStatistics = new ComponentStatistics();
    	this.executionTracker = executionTracker;
        this.resourceFactory = resourceFactory;

        Resource resource = flowStep.getComponentVersion().getResource();
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
    
    public void setFlowStep(FlowStep flowStep) {
    	this.flowStep = flowStep;
    }

}
