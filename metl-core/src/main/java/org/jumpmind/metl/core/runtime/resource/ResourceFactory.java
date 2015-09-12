package org.jumpmind.metl.core.runtime.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.AbstractFactory;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.properties.TypedProperties;

public class ResourceFactory extends AbstractFactory<IResourceRuntime> implements IResourceFactory {

    Map<String, Class<? extends IResourceRuntime>> resourceTypes;

    Map<ResourceCategory, List<String>> categoryToTypeMapping;

    public ResourceFactory() {
        super(IResourceRuntime.class);
    }

    @Override
    public List<String> getResourceTypes() {
        return new ArrayList<String>(resourceTypes.keySet());
    }
    
    @Override
    public List<String> getResourceTypes(ResourceCategory category) {
        return categoryToTypeMapping.get(category);
    }
    
    @Override
    public void register(Class<IResourceRuntime> clazz) {
        ResourceDefinition definition = clazz.getAnnotation(ResourceDefinition.class);
        if (definition != null) {
            if (resourceTypes == null) {
                resourceTypes = new LinkedHashMap<String, Class<? extends IResourceRuntime>>();
            }
            resourceTypes.put(definition.typeName(), clazz);
            
            if (categoryToTypeMapping == null) {
                categoryToTypeMapping = new LinkedHashMap<ResourceCategory, List<String>>();
            }
            List<String> types = categoryToTypeMapping.get(definition.resourceCategory());
            if (types == null) {
                types = new ArrayList<String>();
                categoryToTypeMapping.put(definition.resourceCategory(), types);
            }
            types.add(definition.typeName());
            
            types = categoryToTypeMapping.get(ResourceCategory.ANY);
            if (types == null) {
                types = new ArrayList<String>();
                categoryToTypeMapping.put(ResourceCategory.ANY, types);
            }
            types.add(definition.typeName());
            
        } else {
            throw new IllegalStateException("A resource is required to define the "
                    + ResourceDefinition.class.getName() + " annotation");
        }
    }

    @Override
    public IResourceRuntime create(Resource resource, TypedProperties agentOverrides) {
        try {
            String resourceType = resource.getType();
            Class<? extends IResourceRuntime> clazz = resourceTypes.get(resourceType);
            if (clazz != null) {
                IResourceRuntime runtime = clazz.newInstance();
                runtime.start(this, resource, agentOverrides);
                return runtime;
            } else {
                throw new IllegalStateException(
                        "Could not find a class associated with the resource type of "
                                + resourceType);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Map<String, SettingDefinition> getSettingDefinitionsForResourceType(
            String resourceType) {
        Class<? extends IResourceRuntime> clazz = resourceTypes.get(resourceType);
        return AbstractResourceRuntime.getSettingDefinitions(clazz, false);
    }

}
