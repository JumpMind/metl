package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;

public interface IComponentFactory {

    public IComponentRuntime create(String type);

    public void register(Class<IComponentRuntime> clazz);

    public Map<ComponentCategory, List<String>> getComponentTypes();
    
    public Map<String, SettingDefinition> getSettingDefinitionsForComponentType(String componentType);
    
    public ComponentDefinition getComponentDefinitionForComponentType(String componentType);

}
