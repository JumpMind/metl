package org.jumpmind.metl.core.runtime.web;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

public class HttpRequestMappingRegistry implements IHttpRequestMappingRegistry {

    final Logger log = LoggerFactory.getLogger(getClass());

    AntPathMatcher patternMatcher = new AntPathMatcher();
    
    Map<HttpMethod, Set<HttpRequestMapping>> mappingsByHttpMethod = new HashMap<>();
    
    @Override
    public HttpRequestMapping findBestMatch(HttpMethod method, String path) {
        Set<HttpRequestMapping> mappings = mappingsByHttpMethod.get(method);
        if (mappings != null) {
            for (HttpRequestMapping httpRequestMapping : mappings) {
                log.info(String.format("Found available uri: %s",httpRequestMapping.getPath()));
                if (patternMatcher.match(httpRequestMapping.getPath(), path)) {
                    return httpRequestMapping;
                }
            }
        }
        return null;
    }
    
    @Override
    public void register(HttpRequestMapping request) {
        Set<HttpRequestMapping> mappings = mappingsByHttpMethod.get(request.getMethod());
        if (mappings == null) {
            mappings = new TreeSet<>();
            mappingsByHttpMethod.put(request.getMethod(), mappings);
        }
        log.info("Registering REST service: {}", request);
        mappings.add(request);        
    }
    
    @Override
    public void unregister(HttpRequestMapping request) {
        Set<HttpRequestMapping> mappings = mappingsByHttpMethod.get(request.getMethod());
        if (mappings != null) {
            log.info("Unregistering REST service: {}", request);
            mappings.remove(request);
        }
    }
    
}
