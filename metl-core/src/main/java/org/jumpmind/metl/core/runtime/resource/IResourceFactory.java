package org.jumpmind.metl.core.runtime.resource;

import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.properties.TypedProperties;

public interface IResourceFactory {

    public IResourceRuntime create(Resource resource, TypedProperties agentOverrides);

    public void register(Class<IResourceRuntime> clazz);

    public List<String> getResourceTypes();
    
    public List<String> getResourceTypes(ResourceCategory category);
    
    public Map<String, SettingDefinition> getSettingDefinitionsForResourceType(String resourceType);

}
