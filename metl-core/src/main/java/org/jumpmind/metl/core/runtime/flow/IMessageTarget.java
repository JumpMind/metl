package org.jumpmind.metl.core.runtime.flow;

import org.jumpmind.metl.core.runtime.Message;

public interface IMessageTarget {

    public void put(Message message);
    
}
