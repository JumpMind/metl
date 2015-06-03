package org.jumpmind.symmetric.is.core.runtime.resource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    
    public Map<String, SettingDefinition> getSettingDefinitions() {
        return getSettingDefinitions(false);
    }

    public Map<String, SettingDefinition> getProvidedSettingDefinitions() {
        return getSettingDefinitions(true);
    }

    public Map<String, SettingDefinition> getSettingDefinitions(boolean provided) {
        return getSettingDefinitions(getClass(), provided);
    }

    public static Map<String, SettingDefinition> getSettingDefinitions(Class<?> clazz, boolean provided) {
        try {
            List<Field> fields = new ArrayList<Field>();
            if (clazz != null) {
                do {
                    fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
                    clazz = clazz.getSuperclass();
                } while (clazz != null);

                Map<String, SettingDefinition> map = new HashMap<String, SettingDefinition>();
                for (Field field : fields) {
                    try {
                        SettingDefinition definition = field.getAnnotation(SettingDefinition.class);
                        if (definition != null && definition.provided() == provided) {
                            String property = (String) field.get(null);
                            map.put(property, definition);
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                return order(map);
            } else {
                return Collections.emptyMap();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, SettingDefinition> order(Map<String, SettingDefinition> definitions) {
        ArrayList<SettingDefinitionHolder> orderer = new ArrayList<SettingDefinitionHolder>();
        for (Map.Entry<String, SettingDefinition> definition : definitions.entrySet()) {
            orderer.add(new SettingDefinitionHolder(definition.getKey(), definition.getValue()));
        }
        Collections.sort(orderer);

        LinkedHashMap<String, SettingDefinition> map = new LinkedHashMap<String, SettingDefinition>();
        for (SettingDefinitionHolder settingDefinitionHolder : orderer) {
            map.put(settingDefinitionHolder.name, settingDefinitionHolder.definition);
        }
        return map;
    }

    static class SettingDefinitionHolder implements Comparable<SettingDefinitionHolder> {

        String name;
        SettingDefinition definition;

        public SettingDefinitionHolder(String name, SettingDefinition definition) {
            this.definition = definition;
            this.name = name;
        }

        @Override
        public int compareTo(SettingDefinitionHolder o) {
            return new Integer(definition.order()).compareTo(o.definition.order());
        }
    }
    

}
