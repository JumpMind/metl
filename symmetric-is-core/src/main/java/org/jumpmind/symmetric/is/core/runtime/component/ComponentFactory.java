package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;

public class ComponentFactory implements IComponentFactory {

    Map<String, Class<? extends IComponent>> componentTypes = new LinkedHashMap<String, Class<? extends IComponent>>();

    Map<ComponentCategory, List<String>> componentTypesByCategory = new LinkedHashMap<ComponentCategory, List<String>>();

    public ComponentFactory() {
        componentTypesByCategory.put(ComponentCategory.READER, new ArrayList<String>());
        componentTypesByCategory.put(ComponentCategory.PROCESSOR, new ArrayList<String>());
        componentTypesByCategory.put(ComponentCategory.WRITER, new ArrayList<String>());
        register(DbReaderComponent.class);
        register(NoOpProcessorComponent.class);
    }

    @Override
    public Map<ComponentCategory, List<String>> getComponentTypes() {
        return componentTypesByCategory;
    }

    @Override
    public void register(Class<? extends IComponent> clazz) {
        ComponentDefinition definition = clazz.getAnnotation(ComponentDefinition.class);
        if (definition != null) {
            componentTypes.put(definition.typeName(), clazz);
            List<String> types = componentTypesByCategory.get(definition.category());
            if (types == null) {
                types = new ArrayList<String>();
                componentTypesByCategory.put(definition.category(), types);
            }
            types.add(definition.typeName());
        } else {
            throw new IllegalStateException("A component is required to define the "
                    + ComponentDefinition.class.getName() + " annotation");
        }
    }

    @Override
    public IComponent create(ComponentFlowNode componentFlowNode) {
        try {
            String componentType = componentFlowNode.getComponentVersion().getComponent().getData().getType();
            Class<? extends IComponent> clazz = componentTypes.get(componentType);
            if (clazz != null) {
            	IComponent component = clazz.newInstance();
            	component.setComponentFlowNode(componentFlowNode);
                return component;
            } else {
                throw new IllegalStateException(
                        "Could not find a class associated with the component type of "
                                + componentType);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, SettingDefinition> getSettingDefinitionsForComponentType(String componentType) {
        Class<? extends IComponent> clazz = componentTypes.get(componentType);
        return AbstractRuntimeObject.getSettingDefinitions(clazz, false);
    }
    
    @Override
    public ComponentDefinition getComponentDefinitionForComponentType(String componentType) {
        Class<? extends IComponent> clazz = componentTypes.get(componentType);
        return clazz.getAnnotation(ComponentDefinition.class);
    }
    
}
