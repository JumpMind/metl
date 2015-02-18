package org.jumpmind.symmetric.is.core.config;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;

abstract public class DeprecatedAbstractObjectWithSettings<D extends AbstractData> extends DeprecatedAbstractObject<D> {

    private static final long serialVersionUID = 1L;

    protected List<Setting> settings;

    public DeprecatedAbstractObjectWithSettings(D data, Setting... settings) {
        super(data);
        this.settings = new ArrayList<Setting>();
        if (settings != null) {
            for (Setting settingData : settings) {
                this.settings.add(settingData);
            }
        }
    }
    
    public void put(String name, String value) {
        for (Setting settingData : settings) {
            if (name.equals(settingData.getName())) {
                settingData.setValue(value);
                return;
            }
        }
        
        Setting settingData = createSettingData();
        settingData.setName(name);
        settingData.setValue(value);
        settings.add(settingData);

    }
    
    @SuppressWarnings("unchecked")
    public void setSettings(List<? extends Setting> settings) {
        this.settings = (List<Setting>)settings;
    }
    
    abstract protected Setting createSettingData();
    
    
    public Setting findSetting(String name) {
        for (Setting settingData : settings) {
            if (name.equals(settingData.getName())) {
                return settingData;
            }
        }
        
        Setting settingData = createSettingData();
        settingData.setName(name);
        settings.add(settingData);
        return settingData;
    }
    
    public String get(String name, String defaultValue) {
        for (Setting settingData : settings) {
            if (name.equals(settingData.getName())) {
                return settingData.getValue();
            }
        }
        return defaultValue;
    }
    
    public String get(String name) {
        return get(name, null);
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

        for (Setting settingObject : settings) {
            properties.setProperty(settingObject.getName(), settingObject.getValue());
        }
        return properties;
    }
    
    public List<Setting> getSettings() {
        return settings;
    }
}
