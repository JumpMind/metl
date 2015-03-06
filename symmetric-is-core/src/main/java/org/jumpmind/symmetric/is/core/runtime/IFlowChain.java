package org.jumpmind.symmetric.is.core.runtime;


public interface IFlowChain {

    public void doNext(Message outputMessage);
    
}
