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
package org.jumpmind.metl.core.runtime.component.definition;

import static org.jumpmind.metl.core.runtime.component.definition.ComponentSettingsConstants.ENABLED;
import static org.jumpmind.metl.core.runtime.component.definition.ComponentSettingsConstants.INBOUND_QUEUE_CAPACITY;
import static org.jumpmind.metl.core.runtime.component.definition.ComponentSettingsConstants.LOG_INPUT;
import static org.jumpmind.metl.core.runtime.component.definition.ComponentSettingsConstants.LOG_OUTPUT;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.PluginArtifactVersion;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.util.AbstractXMLFactory;

public class ComponentXmlDefinitionFactory extends AbstractXMLFactory implements IComponentDefinitionFactory {

    Map<String, XMLComponent> componentsById;

    Map<String, List<String>> componentIdsByCategory;
    
    public ComponentXmlDefinitionFactory() {
        super(null);
    }
    
    public ComponentXmlDefinitionFactory(IPluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    synchronized public Map<String, List<XMLComponent>> getDefinitionsByCategory() {
        Map<String, List<XMLComponent>> componentDefinitionsByCategory = new HashMap<>();
        Set<String> categories = componentIdsByCategory.keySet();
        for (String category : categories) {
            List<XMLComponent> list = new ArrayList<>();
            componentDefinitionsByCategory.put(category, list);
            List<String> types = componentIdsByCategory.get(category);
            for (String type : types) {
                list.add(componentsById.get(type));
            }
        }
        return componentDefinitionsByCategory;
    }

    @Override
    synchronized public Map<String, List<String>> getTypesByCategory() {
        return componentIdsByCategory;
    }

    @Override
    synchronized public XMLComponent getDefinition(String id) {
        return componentsById.get(id);
    }

    @Override
    protected void reset() {
        componentIdsByCategory = new HashMap<String, List<String>>();
        componentsById = new HashMap<String, XMLComponent>();
    }

    @Override
    protected void loadComponentsForClassloader(PluginArtifactVersion pluginArtifactVersion, ClassLoader classLoader) {
        try {
            JAXBContext jc = JAXBContext.newInstance(XMLComponents.class.getPackage().getName());
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            List<InputStream> componentXmls = loadResources("components.xml", classLoader);
            try {
                for (InputStream inputStream : componentXmls) {
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    @SuppressWarnings("unchecked")
                    JAXBElement<XMLComponents> root = (JAXBElement<XMLComponents>) unmarshaller.unmarshal(reader);
                    XMLComponents components = root.getValue();
                    List<XMLComponent> componentList = components.getComponent();
                    for (XMLComponent xmlComponent : componentList) {
                        String id = xmlComponent.getId();
                        if (componentsById.containsKey(id)) {
                            log.warn("There was already a component registered under the id of {}.  Overwriting {} with {}",
                                    new Object[] { id, componentsById.get(id).getClassName(), xmlComponent.getClassName() });
                        }
                        componentsById.put(id, xmlComponent);

                        List<String> ids = componentIdsByCategory.get(xmlComponent.getCategory());
                        if (ids == null) {
                            ids = new ArrayList<String>();
                            componentIdsByCategory.put(xmlComponent.getCategory(), ids);
                        }

                        if (!ids.contains(xmlComponent.getId())) {
                            ids.add(xmlComponent.getId());
                        }

                        if (xmlComponent.getSettings() == null) {
                            xmlComponent.setSettings(new XMLSettings());
                        }

                        if (xmlComponent.getSettings().getSetting() == null) {
                            xmlComponent.getSettings().setSetting(new ArrayList<XMLSetting>());
                        }

                        xmlComponent.getSettings().getSetting().add(0, new XMLSetting(ENABLED, "Enabled", "true", Type.BOOLEAN, true));
                        xmlComponent.getSettings().getSetting().add(new XMLSetting(LOG_INPUT, "Log Input", "false", Type.BOOLEAN, false));
                        xmlComponent.getSettings().getSetting().add(new XMLSetting(LOG_OUTPUT, "Log Output", "false", Type.BOOLEAN, false));
                        xmlComponent.getSettings().getSetting()
                                .add(new XMLSetting(INBOUND_QUEUE_CAPACITY, "Inbound Queue Capacity", "100", Type.INTEGER, true));
                    }
                }
            } finally {
                for (InputStream inputStream : componentXmls) {
                    IOUtils.closeQuietly(inputStream);
                }

            }
        } catch (Exception e) {
            throw new IoException(e);
        }
    }

}
