package org.jumpmind.symmetric.is.core.runtime.flow;

import org.jumpmind.symmetric.is.core.runtime.Message;

public interface IMessageTarget {

    public void put(Message message);
    
}
