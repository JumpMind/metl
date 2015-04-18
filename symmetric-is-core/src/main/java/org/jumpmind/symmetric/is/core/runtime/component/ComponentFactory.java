package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.AbstractFactory;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;

public class ComponentFactory extends AbstractFactory<IComponent> implements IComponentFactory {

    Map<String, Class<? extends IComponent>> componentTypes;

    Map<ComponentCategory, List<String>> componentTypesByCategory;

    public ComponentFactory() {
        super(IComponent.class);
    }

    @Override
    public Map<ComponentCategory, List<String>> getComponentTypes() {
        return componentTypesByCategory;
    }

    @Override
    public void register(Class<IComponent> clazz) {
        ComponentDefinition definition = clazz.getAnnotation(ComponentDefinition.class);
        if (definition != null) {
            if (componentTypes == null) {
                componentTypes = new LinkedHashMap<String, Class<? extends IComponent>>();
            }
            componentTypes.put(definition.typeName(), clazz);

            if (componentTypesByCategory == null) {
                componentTypesByCategory = new LinkedHashMap<ComponentCategory, List<String>>();
            }
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
    public IComponent create(FlowStep flowStep, Flow flow) {
        try {
            String componentType = flowStep.getComponent().getType();
            Class<? extends IComponent> clazz = componentTypes.get(componentType);
            if (clazz != null) {
                IComponent component = clazz.newInstance();
                component.init(flowStep, flow);
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
        if (clazz != null) {
            return clazz.getAnnotation(ComponentDefinition.class);
        } else {
            throw new IllegalStateException(
                    "Could not find a class associated with the component type of "
                            + componentType);
        }
    }

}
