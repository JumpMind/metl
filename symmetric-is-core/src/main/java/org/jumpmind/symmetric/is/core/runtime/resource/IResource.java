package org.jumpmind.symmetric.is.core.runtime.resource;

import org.jumpmind.symmetric.is.core.model.Resource;

public interface IResource {

    public void start(Resource resource);
    
    public void stop();
    
    public <T> T reference();
    
}
