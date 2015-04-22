package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;

public interface IComponent {

    public void init(FlowStep flowStep, Flow flow, Map<String, IResource> resources);
    
    public void start(String executionId, IExecutionTracker tracker);

    public void handle(Message inputMessage, IMessageTarget messageTarget);
    
    public void flowCompletedWithoutError();
    
    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors);
    
    public void stop();
    
    public ComponentStatistics getComponentStatistics();

    public FlowStep getFlowStep(); 
    
    public Flow getFlow();
    
    public IExecutionTracker getExecutionTracker();
    
    public IResource getResource();
    
    public String getExecutionId();

}
