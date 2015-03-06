package org.jumpmind.symmetric.is.core.runtime.resource;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;

public interface IResourceFactory {

    public IResource create(Resource resource);

    public void register(Class<? extends IResource> clazz);

    public List<String> getResourceTypes();
    
    public List<String> getResourceTypes(ResourceCategory category);
    
    public Map<String, SettingDefinition> getSettingDefinitionsForResourceType(String resourceType);

}
