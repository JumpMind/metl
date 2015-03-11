package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

public interface IComponent {

    public void start(IExecutionTracker tracker, IResourceFactory resourceFactory);

    public void stop();
    
    public ComponentStatistics getComponentStatistics();

    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget);
    
    public FlowStep getFlowStep();
    
    public void setFlowStep(FlowStep flowStep);

}
