package org.jumpmind.symmetric.is.core.config;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;

abstract public class AbstractObjectWithSettings<D extends AbstractData> extends AbstractObject<D> {

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
    
    public void put(String name, String value) {
        for (SettingData settingData : settings) {
            if (name.equals(settingData.getName())) {
                settingData.setValue(value);
                return;
            }
        }
        
        SettingData settingData = createSettingData();
        settingData.setName(name);
        settingData.setValue(value);
        settings.add(settingData);

    }
    
    abstract protected SettingData createSettingData();
    
    public String get(String name) {
        for (SettingData settingData : settings) {
            if (name.equals(settingData.getName())) {
                return settingData.getValue();
            }
        }
        return null;
    }
    
    public boolean getBoolean(String name) {
        String value = get(name);
        if (isBlank(value)) {
            return false;
        } else {
            return Boolean.parseBoolean(value);
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
    
    public List<SettingData> getSettings() {
        return settings;
    }
}
