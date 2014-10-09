package org.jumpmind.symmetric.is.core.config;

import java.util.List;
import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class AbstractObjectWithSettings<D extends AbstractData> extends AbstractObject<D> {

    private static final long serialVersionUID = 1L;
    
    protected List<SettingData> settings;
    
    public AbstractObjectWithSettings(SettingData... settings) {
    }

    protected void initSettings(Map<String, SettingDefinition> definitions, SettingData... settings) {
        for (String name : definitions.keySet()) {
            String defaultValue = definitions.get(name).defaultValue();
            this.settings.add(new SettingData(name, defaultValue));
        }

        if (settings != null) {
            for (SettingData setting : settings) {
                this.settings.remove(setting);
                this.settings.add(setting);
            }
        }
    }



    public TypedProperties toTypedProperties() {
        TypedProperties properties = new TypedProperties();
        for (SettingData settingObject : settings) {
            properties.setProperty(settingObject.getName(), settingObject.getValue());
        }
        return properties;
    }
}
