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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.ProjectVersionComponentPlugin;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentXmlDefinitionFactory implements IComponentDefinitionFactory {

    final Logger logger = LoggerFactory.getLogger(getClass());

    static Set<Plugin> outOfTheBox = new TreeSet<>();

    static {
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-rdbms-reader"));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-data-diff"));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-sorter"));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-temp-rdbms"));
    }

    Map<String, XMLComponent> componentsById;

    Map<String, List<String>> componentIdsByCategory;

    Map<String, List<XMLComponent>> componentsByPluginId;

    IConfigurationService configurationService;

    IPluginManager pluginManager;

    public ComponentXmlDefinitionFactory() {
        componentsById = new HashMap<>();
        componentsByPluginId = new HashMap<>();
        componentIdsByCategory = new HashMap<>();
    }

    public ComponentXmlDefinitionFactory(IConfigurationService configurationService, IPluginManager pluginManager) {
        this();
        this.configurationService = configurationService;
        this.pluginManager = pluginManager;
    }
    
    @Override
    synchronized public void init() {
        componentsById = new HashMap<>();
        componentsByPluginId = new HashMap<>();
        componentIdsByCategory = new HashMap<>();
        loadComponentsForClassloader("org.jumpmind.metl:metl-core:" + VersionUtils.getCurrentVersion(), getClass().getClassLoader());
        List<String> projectVersionIds = configurationService.findAllProjectVersionIds();
        for (String projectVersionId : projectVersionIds) {
            List<ProjectVersionComponentPlugin> pvcps = configurationService.findProjectVersionComponentPlugin(projectVersionId);
            for (Plugin ootbp : outOfTheBox) {
                boolean matched = false;
                for (ProjectVersionComponentPlugin pvcp : pvcps) {
                    if (pvcp.matches(ootbp)) {
                        matched = true;
                        String latestVersion = pluginManager.getLatestLocalVersion(pvcp.getArtifactGroup(), pvcp.getArtifactName());
                        if (!pvcp.getArtifactVersion().equals(latestVersion)) {
                            if (!pvcp.isPinVersion()) {
                                logger.info("Upgrading from {}:{}:{} to {}", pvcp.getArtifactGroup(), pvcp.getArtifactName(),
                                        pvcp.getArtifactVersion(), latestVersion);
                                pvcp.setArtifactVersion(latestVersion);
                            } else {
                                logger.info("Not upgrading from {}:{}:{} to {} because the version is pinned", pvcp.getArtifactGroup(),
                                        pvcp.getArtifactName(), pvcp.getArtifactVersion(), latestVersion);
                                pvcp.setLatestArtifactVersion(latestVersion);
                            }
                            configurationService.save(pvcp);
                        }
                        
                        load(pvcp.getArtifactGroup(), pvcp.getArtifactName(),
                                pvcp.getArtifactVersion());
                    }
                }

                if (!matched) {
                    String latestVersion = pluginManager.getLatestLocalVersion(ootbp.getArtifactGroup(), ootbp.getArtifactName());
                    if (latestVersion != null) {
                        String pluginId = load(ootbp.getArtifactGroup(), ootbp.getArtifactName(),
                                latestVersion);

                        List<XMLComponent> components = componentsByPluginId.get(pluginId);
                        for (XMLComponent xmlComponent : components) {
                            ProjectVersionComponentPlugin plugin = new ProjectVersionComponentPlugin();
                            plugin.setProjectVersionId(projectVersionId);
                            plugin.setComponentTypeId(xmlComponent.getId());
                            plugin.setArtifactGroup(ootbp.getArtifactGroup());
                            plugin.setArtifactName(ootbp.getArtifactName());
                            plugin.setArtifactVersion(latestVersion);
                            plugin.setLatestArtifactVersion(latestVersion);
                            configurationService.save(plugin);
                        }

                    } else {
                        logger.warn("Could not find a registered plugin for {}:{}", ootbp.getArtifactGroup(), ootbp.getArtifactName());
                    }
                }

            }
        }

    }
    
    protected String load(String artifactGroup, String artifactName, String artifactVersion) {
        ClassLoader classLoader = pluginManager.getClassLoader(artifactGroup, artifactName, artifactVersion);
        String pluginId = toPluginId(artifactGroup, artifactName, artifactVersion);
        loadComponentsForClassloader(pluginId, classLoader);
        return pluginId;
    }

    @Override
    synchronized public Map<String, List<XMLComponent>> getDefinitionsByCategory(String projectVersionId) {
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

    synchronized public Map<String, List<String>> getTypesByCategory() {
        return componentIdsByCategory;
    }

    @Override
    synchronized public XMLComponent getDefinition(String projectVersionId, String id) {
        return componentsById.get(id);
    }

    protected void reset() {
        componentsByPluginId = new HashMap<>();
        componentIdsByCategory = new HashMap<>();
        componentsById = new HashMap<>();
    }

    protected String toPluginId(String artifactGroup, String artifactName, String artifactVersion) {
        return String.format("%s:%s:%s", artifactGroup, artifactName, artifactVersion);
    }

    protected void loadComponentsForClassloader(String pluginId, ClassLoader classLoader) {
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
                        if (!componentsById.containsKey(id)) {
                            xmlComponent.setClassLoader(classLoader);
                            componentsById.put(id, xmlComponent);

                            List<XMLComponent> componentsForPluginId = componentsByPluginId.get(pluginId);
                            if (componentsForPluginId == null) {
                                componentsForPluginId = new ArrayList<>();
                                componentsByPluginId.put(pluginId, componentsForPluginId);
                            }
                            componentsForPluginId.add(xmlComponent);
                            
                            logger.info("Registering '{}' with an id of '{}' for plugin {}", xmlComponent.getName(), id, pluginId);

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
                            xmlComponent.getSettings().getSetting()
                                    .add(new XMLSetting(LOG_OUTPUT, "Log Output", "false", Type.BOOLEAN, false));
                            xmlComponent.getSettings().getSetting()
                                    .add(new XMLSetting(INBOUND_QUEUE_CAPACITY, "Inbound Queue Capacity", "100", Type.INTEGER, true));
                        } else {
                            if (!getClass().getClassLoader().equals(componentsById.get(id).getClassLoader())) {
                               logger.info("There was already a component registered under the id of {} with the name {}", new Object[] { id, xmlComponent.getName() });
                            }
                        }
                    }
                }
            } finally {
                for (InputStream inputStream : componentXmls) {
                    IOUtils.closeQuietly(inputStream);
                }

            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<InputStream> loadResources(final String name, final ClassLoader classLoader) {
        try {
            Set<URL> urls = new HashSet<>();
            final List<InputStream> list = new ArrayList<InputStream>();
            final Enumeration<URL> systemResources = (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
                    .getResources(name);
            while (systemResources.hasMoreElements()) {
                URL url = systemResources.nextElement();
                if (!urls.contains(url)) {
                    list.add(url.openStream());
                    urls.add(url);
                }
            }
            return list;
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

}
