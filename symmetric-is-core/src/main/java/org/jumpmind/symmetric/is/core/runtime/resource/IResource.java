package org.jumpmind.symmetric.is.core.runtime.resource;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Resource;

public interface IResource {

    public void start(IResourceFactory resourceFactory, Resource resource, TypedProperties agentOverrides);
    
    public void stop();
    
    public <T> T reference();
    
    public Resource getResource();
    
    public TypedProperties getAgentOverrides();
    
}
