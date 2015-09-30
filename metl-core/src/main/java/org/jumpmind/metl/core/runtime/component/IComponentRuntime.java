package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public interface IComponentRuntime {

    public void register(XMLComponent definition);
    
    public void start(ComponentContext context);

    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage);
    
    public void lastMessageReceived(IMessageTarget messageTarget);
    
    public void flowCompleted(boolean cancelled);
    
    public void flowCompletedWithErrors(Throwable myError);
    
    public void stop();   

    public ComponentContext getComponentContext(); 
    
}
