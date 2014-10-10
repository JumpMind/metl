package org.jumpmind.symmetric.is.core.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.runtime.component.DbReaderComponent;

public class ComponentFactory {

    Map<String, Class<? extends IComponent>> componentTypes = new LinkedHashMap<String, Class<? extends IComponent>>();

    public ComponentFactory() {
        register(DbReaderComponent.class);
    }

    public List<String> getComponentTypes() {
        return new ArrayList<String>(componentTypes.keySet());
    }

    public void register(Class<? extends IComponent> clazz) {
        ComponentDefinition definition = clazz.getAnnotation(ComponentDefinition.class);
        if (definition != null) {
            componentTypes.put(definition.typeName(), clazz);
        } else {
            throw new IllegalStateException("A component is required to define the "
                    + ComponentDefinition.class.getName() + " annotation");
        }
    }

    public IComponent create(ComponentVersion componentVersion) {
        try {
            String componentType = componentVersion.getComponent().getData().getType();
            Class<? extends IComponent> clazz = componentTypes.get(componentType);
            if (clazz != null) {
                return clazz.newInstance();
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
}
