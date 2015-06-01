package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.AbstractFactory;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;

public class ComponentFactory extends AbstractFactory<IComponentRuntime> implements IComponentFactory {

    Map<String, Class<? extends IComponentRuntime>> componentTypes;

    Map<ComponentCategory, List<String>> componentTypesByCategory;

    public ComponentFactory() {
        super(IComponentRuntime.class);
    }

    @Override
    public Map<ComponentCategory, List<String>> getComponentTypes() {
        return componentTypesByCategory;
    }

    @Override
    public void register(Class<IComponentRuntime> clazz) {
        ComponentDefinition definition = clazz.getAnnotation(ComponentDefinition.class);
        if (definition != null) {
            if (componentTypes == null) {
                componentTypes = new LinkedHashMap<String, Class<? extends IComponentRuntime>>();
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
            throw new IllegalStateException("A component is required to define the " + ComponentDefinition.class.getName() + " annotation");
        }
    }

    @Override
    public IComponentRuntime create(String componentType) {
        try {
            Class<? extends IComponentRuntime> clazz = componentTypes.get(componentType);
            if (clazz != null) {
                IComponentRuntime component = clazz.newInstance();
                return component;
            } else {
                throw new IllegalStateException("Could not find a class associated with the component type of " + componentType);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, SettingDefinition> getSettingDefinitionsForComponentType(String componentType) {
        Class<? extends IComponentRuntime> clazz = componentTypes.get(componentType);
        return AbstractRuntimeObject.getSettingDefinitions(clazz, false);
    }

    @Override
    public ComponentDefinition getComponentDefinitionForComponentType(String componentType) {
        Class<? extends IComponentRuntime> clazz = componentTypes.get(componentType);
        if (clazz != null) {
            return clazz.getAnnotation(ComponentDefinition.class);
        } else {
            throw new IllegalStateException("Could not find a class associated with the component type of " + componentType);
        }
    }

    public static void main(String[] args) {
        ComponentFactory factory = new ComponentFactory();
        StringBuilder xml = new StringBuilder();

        ArrayList<String> allTypes = new ArrayList<String>();
        Map<ComponentCategory, List<String>> types = factory.getComponentTypes();
        Collection<List<String>> typeStrings = types.values();
        for (List<String> list : typeStrings) {
            allTypes.addAll(list);
        }
        Collections.sort(allTypes);
        for (String type : allTypes) {
            ComponentDefinition definition = factory.getComponentDefinitionForComponentType(type);
            xml.append("\n<component ");
            xml.append("id='").append(definition.typeName()).append("' category='").append(definition.category().name())
                    .append("' inputMessageType='").append(definition.inputMessage().name().toLowerCase()).append("' outputMessageType='")
                    .append(definition.outgoingMessage().name().toLowerCase()).append("' resourceCategory='")
                    .append(definition.resourceCategory().name().toLowerCase()).append("' inputOutputModelsMatch='")
                    .append(definition.inputOutputModelsMatch()).append("'>");
            xml.append("\n    <name>").append(definition.typeName()).append("</name>");
            xml.append("\n    <classname>").append(factory.componentTypes.get(type).getName()).append("</classname>");
            xml.append("\n    <description>").append("</description>");
            xml.append("\n    <settings>");
            Map<String, SettingDefinition> definitions = factory.getSettingDefinitionsForComponentType(type);
            Map<SettingDefinition, String> def2name = new HashMap<SettingDefinition, String>();
            List<SettingDefinition> orderedDefinitions = new ArrayList<SettingDefinition>();
            Set<String> keys = definitions.keySet();
            for (String key : keys) {
                SettingDefinition def = definitions.get(key);
                orderedDefinitions.add(def);
                def2name.put(def, key);
            }

            Collections.sort(orderedDefinitions, new Comparator<SettingDefinition>() {
                @Override
                public int compare(SettingDefinition o1, SettingDefinition o2) {
                    return new Integer(o1.order()).compareTo(new Integer(o2.order()));
                }
            });

            for (SettingDefinition def : orderedDefinitions) {
                String key = def2name.get(def);
                if (!"enabled".equals(key) && !"inbound.queue.capacity".equals(key)) {
                    xml.append("\n        <setting id='").append(key).append("' type='").append(def.type().name().toLowerCase())
                            .append("' required='").append(def.required()).append("'>");
                    xml.append("\n            <name>").append(def.label()).append("</name>");
                    if (isNotBlank(def.defaultValue())) {
                        xml.append("\n            <defaultValue>").append(def.defaultValue()).append("</defaultValue>");
                    }
                    xml.append("\n            <description>").append("</description>");
                    if (def.type() == Type.CHOICE) {
                        xml.append("\n            <choices>");
                        String[] choices = def.choices();
                        for (String string : choices) {
                            xml.append("\n                <choice>").append(string).append("</choice>");
                        }
                        xml.append("\n            </choices>");
                    }
                    xml.append("\n        </setting>");
                }
            }
            xml.append("\n    </settings>");
            xml.append("\n</component>");
        }

        System.out.println(xml);
    }

}
