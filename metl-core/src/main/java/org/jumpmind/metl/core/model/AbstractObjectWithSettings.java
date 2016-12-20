/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.model;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.plugin.XMLSetting;
import org.jumpmind.properties.TypedProperties;

abstract public class AbstractObjectWithSettings extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    protected List<Setting> settings;

    public AbstractObjectWithSettings(Setting... settings) {
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
        this.settings = (List<Setting>) settings;
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

    public Setting findSetting(String name, String value) {
        for (Setting settingData : settings) {
            if (name.equals(settingData.getName()) && StringUtils.equals(value, settingData.getValue())) {
                return settingData;
            }
        }

        Setting settingData = createSettingData();
        settingData.setName(name);
        settingData.setValue(value);
        settings.add(settingData);
        return settingData;
    }

    public List<String> getList(String name) {
        List<String> list = new ArrayList<String>();
        for (Setting settingData : settings) {
            if (name.equals(settingData.getName())) {
                list.add(settingData.getValue());
            }
        }
        return list;
    }

    public String get(String name, String defaultValue) {
        String value = null;
        for (Setting settingData : settings) {
            if (name.equals(settingData.getName())) {
                value = settingData.getValue();
                break;
            }
        }
        return value != null ? value : defaultValue;
    }

    public long getLong(String name, long defaultValue) {
        String value = get(name);
        if (value != null) {
            return Long.parseLong(value);
        } else {
            return defaultValue;
        }
    }

    public int getInt(String name, int defaultValue) {
        String value = get(name);
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            return defaultValue;
        }
    }

    public String get(String name) {
        return get(name, null);
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        String value = get(name);
        if (isBlank(value)) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    public TypedProperties toTypedProperties(List<XMLSetting> definitions) {
        TypedProperties properties = new TypedProperties();
        for (XMLSetting definition : definitions) {
            properties.put(definition.getId(), definition.getDefaultValue());
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
