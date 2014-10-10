package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;

public interface IComponentListener {

    public void beforeHandle(IComponent runtime, Message<?> inputMessage,
            ComponentFlowNode inputLink);

    public void afterHandle(IComponent runtime, Message<?> inputMessage, ComponentFlowNode sourceNode);

}
