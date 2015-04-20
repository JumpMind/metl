package org.jumpmind.symmetric.is.core.runtime.resource;

import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;

public abstract class AbstractResource extends AbstractRuntimeObject implements IResource {

    protected Resource resource;
    protected TypedProperties agentOverrides;

    @Override
    public void start(IResourceFactory resourceFactory, Resource resource,
            TypedProperties agentOverrides) {
        this.resource = resource;
        this.agentOverrides = agentOverrides;
        Map<String, SettingDefinition> settings = resourceFactory
                .getSettingDefinitionsForResourceType(resource.getType());
        TypedProperties defaultSettings = resource.toTypedProperties(settings);
        TypedProperties combined = new TypedProperties(defaultSettings);
        if (agentOverrides != null) {
            combined.putAll(agentOverrides);
        }
        start(combined);
    }

    abstract protected void start(TypedProperties properties);

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public TypedProperties getAgentOverrides() {
        return agentOverrides;
    }

}
