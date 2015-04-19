package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

public interface IComponent {

    public void init(FlowStep flowStep, Flow flow);
    
    public void start(IExecutionTracker tracker, IResourceFactory resourceFactory);

    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget);
    
    public void flowCompletedWithoutError();
    
    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors);
    
    public void stop();
    
    public ComponentStatistics getComponentStatistics();

    public FlowStep getFlowStep();    

}
