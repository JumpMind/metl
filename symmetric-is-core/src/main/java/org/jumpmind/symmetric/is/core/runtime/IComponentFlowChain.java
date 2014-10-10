package org.jumpmind.symmetric.is.core.runtime;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;

public interface IComponentFlowChain {

    public void doNext(Message<?> outputMessage, List<ComponentFlowNode> outputLinks);
    
}
