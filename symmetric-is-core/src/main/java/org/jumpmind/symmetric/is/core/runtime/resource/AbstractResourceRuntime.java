package org.jumpmind.symmetric.is.core.runtime.resource;

import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;

public abstract class AbstractResourceRuntime extends AbstractRuntimeObject implements IResourceRuntime {

    protected Resource resource;
    protected TypedProperties resourceRuntimeSettings;

    @Override
    public void start(IResourceFactory resourceFactory, Resource resource,
            TypedProperties overrides) {
        this.resource = resource;
        this.resourceRuntimeSettings = overrides;
        Map<String, SettingDefinition> settings = resourceFactory
                .getSettingDefinitionsForResourceType(resource.getType());
        TypedProperties defaultSettings = resource.toTypedProperties(settings);
        TypedProperties combined = new TypedProperties(defaultSettings);
        if (overrides != null) {
            combined.putAll(overrides);
        }
        resourceRuntimeSettings = combined;
        start(combined);
    }

    abstract protected void start(TypedProperties properties);

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public TypedProperties getResourceRuntimeSettings() {
        return resourceRuntimeSettings;
    }

}
