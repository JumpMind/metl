package org.jumpmind.metl.core.runtime;

import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;

public interface IHttpRequestMappingRegistryAware {

    public void setHttpRequestMappingRegistry(IHttpRequestMappingRegistry registry);
    
}
