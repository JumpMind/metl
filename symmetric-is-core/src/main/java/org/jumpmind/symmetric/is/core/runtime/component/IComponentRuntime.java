package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

public interface IComponentRuntime {

    public void init(ComponentContext context);
    
    public void start();

    public void handle(Message inputMessage, IMessageTarget messageTarget);
    
    public void lastMessageReceived(IMessageTarget messageTarget);
    
    public void flowCompleted();
    
    public void flowCompletedWithErrors(Throwable myError);
    
    public void stop();
    
    public ComponentStatistics getComponentStatistics();

    public ComponentContext getComponentContext(); 
    
}
