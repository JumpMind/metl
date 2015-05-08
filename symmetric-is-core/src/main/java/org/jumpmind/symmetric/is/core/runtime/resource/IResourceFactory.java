package org.jumpmind.symmetric.is.core.runtime.resource;

import java.util.List;
import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;

public interface IResourceFactory {

    public IResourceRuntime create(Resource resource, TypedProperties agentOverrides);

    public void register(Class<IResourceRuntime> clazz);

    public List<String> getResourceTypes();
    
    public List<String> getResourceTypes(ResourceCategory category);
    
    public Map<String, SettingDefinition> getSettingDefinitionsForResourceType(String resourceType);

}
