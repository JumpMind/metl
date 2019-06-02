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
package org.jumpmind.metl.core.plugin;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.jumpmind.properties.TypedProperties;

@XmlAccessorType(XmlAccessType.FIELD)
abstract public class XMLAbstractDefinition implements Serializable, Comparable<XMLAbstractDefinition> {

    private static final long serialVersionUID = 1L;
    
    @XmlElement(required = false)
    protected String description;

    @XmlElement(required = true)
    protected String name;

    @XmlElement(required = true)
    protected String className;
    
    @XmlAttribute(required = true)
    protected String id;
        
    protected ClassLoader classLoader;
    
    @XmlElement
    protected XMLSettings settings;
    
    public XMLAbstractDefinition() {
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }    
    
    public void setSettings(XMLSettings settings) {
        this.settings = settings;
    }

    public XMLSettings getSettings() {
        return settings;
    }
    
    public XMLSetting findXMLSetting(String type) {
        if (settings != null) {
            for (XMLSetting setting :settings.getSetting()) {
                if (setting.getId().equals(type)) {
                    return setting;
                }
            }
        }
        return null;
    }       
    
    @Override
    public int compareTo(XMLAbstractDefinition o) {
        return name.compareTo(o.getName());
    }
    
    public TypedProperties toDefaultSettings() {
        TypedProperties properties = new TypedProperties();
        if (settings != null) {
            for (XMLSetting setting :settings.getSetting()) {
                properties.put(setting.getName(), setting.getDefaultValue());
            }
        }
        return properties;
    }

    



}
