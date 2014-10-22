package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;

public class AbstractObjectWithSettings<D extends AbstractData> extends AbstractObject<D> {

    private static final long serialVersionUID = 1L;

    protected List<SettingData> settings;

    public AbstractObjectWithSettings(D data, SettingData... settings) {
        super(data);
        this.settings = new ArrayList<SettingData>();
        if (settings != null) {
            for (SettingData settingData : settings) {
                this.settings.add(settingData);
            }
        }
    }

    public TypedProperties toTypedProperties(AbstractRuntimeObject object, boolean provided) {
        TypedProperties properties = new TypedProperties();

        Map<String, SettingDefinition> definitions = object.getSettingDefinitions(provided);
        for (String name : definitions.keySet()) {
            properties.put(name, definitions.get(name).defaultValue());
        }

        for (SettingData settingObject : settings) {
            properties.setProperty(settingObject.getName(), settingObject.getValue());
        }
        return properties;
    }
}
