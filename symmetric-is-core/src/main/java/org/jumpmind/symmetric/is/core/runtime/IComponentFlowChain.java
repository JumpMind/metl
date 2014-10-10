package org.jumpmind.symmetric.is.core.runtime;


public interface IComponentFlowChain {

    public void doNext(Message outputMessage);
    
}
