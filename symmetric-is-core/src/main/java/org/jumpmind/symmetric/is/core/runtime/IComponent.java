package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;

public interface IComponent {

    public void start(ComponentFlowNode componentNode, IComponentFlowChain chain);

    public void stop();

    public void handle(Message<?> inputMessage, ComponentFlowNode inputLink);

}
