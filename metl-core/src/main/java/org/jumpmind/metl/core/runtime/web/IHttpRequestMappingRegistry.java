package org.jumpmind.metl.core.runtime.web;

import java.util.List;

import org.jumpmind.metl.core.model.AgentDeployment;

public interface IHttpRequestMappingRegistry {
    
    public HttpRequestMapping findBestMatch(HttpMethod method, String path);
    
    public void register(HttpRequestMapping request);
    
    public void unregister(HttpRequestMapping request);
    
    public List<HttpRequestMapping> getHttpRequestMappingsFor(AgentDeployment deployment);

}
