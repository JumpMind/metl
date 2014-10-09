package org.jumpmind.symmetric.is.core.runtime;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.ComponentVersion;

public interface IComponentFlowChain {

    public void doNext(Message<?> outputMessage, List<ComponentVersion> outputLinks);
    
}
