package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.runtime.IComponentFlowChain;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;

public interface IComponent {

    public void start(IExecutionTracker tracker, IConnectionFactory connectionFactory, ComponentFlowNode componentNode, IComponentFlowChain chain);

    public void stop();

    public void handle(Message inputMessage, ComponentFlowNode inputLink);

}
