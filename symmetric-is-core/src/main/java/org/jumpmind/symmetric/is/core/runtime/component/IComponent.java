package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.model.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

public interface IComponent {

    public void start(IExecutionTracker tracker, IConnectionFactory connectionFactory);

    public void stop();
    
    public ComponentStatistics getComponentStatistics();

    public void handle(Message inputMessage, IMessageTarget messageTarget);
    
    public ComponentFlowNode getComponentFlowNode();
    
    public void setComponentFlowNode(ComponentFlowNode componentFlowNode);

}
