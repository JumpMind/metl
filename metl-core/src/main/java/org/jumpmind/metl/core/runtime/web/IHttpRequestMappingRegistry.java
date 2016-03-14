package org.jumpmind.metl.core.runtime.web;

public interface IHttpRequestMappingRegistry {
    
    public HttpRequestMapping findBestMatch(HttpMethod method, String path);
    
    public void register(HttpRequestMapping request);
    
    public void unregister(HttpRequestMapping request);

}
