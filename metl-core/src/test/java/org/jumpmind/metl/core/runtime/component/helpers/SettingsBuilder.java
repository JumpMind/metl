package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Setting;

public class SettingsBuilder {
	List<Setting> settings = new ArrayList<Setting>();
	
	public SettingsBuilder() {
	}
	
	public SettingsBuilder withSetting(String key, String value) {
		Setting s = new Setting(key, value);
		settings.add(s);
		return this;
	}
	
	public List<Setting> build() {
		return this.settings;
	}
}
