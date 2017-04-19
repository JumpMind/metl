package org.jumpmind.metl.core.runtime.resource;

import javax.jms.MessageListener;

public interface ISubscribe {

    public void start(MessageListener listener);
    
    public void stop(MessageListener listener);
    
}
