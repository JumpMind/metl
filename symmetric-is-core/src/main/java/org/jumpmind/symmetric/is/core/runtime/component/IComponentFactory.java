package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;

public interface IComponentFactory {

    public IComponent create(FlowStep flowStep, Flow flow, Map<String, IResource> resources);

    public void register(Class<IComponent> clazz);

    public Map<ComponentCategory, List<String>> getComponentTypes();
    
    public Map<String, SettingDefinition> getSettingDefinitionsForComponentType(String componentType);
    
    public ComponentDefinition getComponentDefinitionForComponentType(String componentType);

}
