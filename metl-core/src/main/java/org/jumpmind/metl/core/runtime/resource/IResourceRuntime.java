package org.jumpmind.metl.core.runtime.resource;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.properties.TypedProperties;

public interface IResourceRuntime {

    public void start(IResourceFactory resourceFactory, Resource resource, TypedProperties agentOverrides);
    
    public void stop();
    
    public <T> T reference();
    
    public Resource getResource();
    
    public TypedProperties getResourceRuntimeSettings();
    
}
