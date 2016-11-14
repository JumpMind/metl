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

import static org.jumpmind.metl.core.runtime.component.definition.ComponentSettingsConstants.*;
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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.model.ProjectVersionComponentPlugin;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentXmlDefinitionFactory implements IComponentDefinitionFactory {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    Map<String, Map<String, XMLComponent>> componentsByProjectVersionIdById;

    Map<String, List<XMLComponent>> componentsByPluginId;

    Map<String, List<String>> componentIdsByCategory;

    protected IConfigurationService configurationService;

    protected IPluginManager pluginManager;

    public ComponentXmlDefinitionFactory() {
        componentsByProjectVersionIdById = new HashMap<>();
        componentsByPluginId = new HashMap<>();
    }

    public ComponentXmlDefinitionFactory(IConfigurationService configurationService, IPluginManager pluginManager) {
        this();
        this.configurationService = configurationService;
        this.pluginManager = pluginManager;
    }

    @Override
    synchronized public void refresh() {
        pluginManager.refresh();
        componentsByProjectVersionIdById = new HashMap<>();
        componentsByPluginId = new HashMap<>();
        if (pluginManager != null && configurationService != null) {
            List<String> projectVersionIds = configurationService.findAllProjectVersionIds();
            for (String projectVersionId : projectVersionIds) {
                refresh(projectVersionId);
            }
        }
    }

    @Override
    public void refresh(String projectVersionId) {
        loadComponentsForClassloader(projectVersionId, "org.jumpmind.metl:metl-core:" + VersionUtils.getCurrentVersion(),
                getClass().getClassLoader());
        List<PluginRepository> remoteRepostiories = configurationService.findPluginRepositories();
        List<ProjectVersionComponentPlugin> pvcps = configurationService.findProjectVersionComponentPlugins(projectVersionId);
        GenericVersionScheme versionScheme = new GenericVersionScheme();
        for (Plugin configuredPlugin : configurationService.findPlugins()) {
            boolean matched = false;
            for (ProjectVersionComponentPlugin pvcp : pvcps) {
                if (pvcp.matches(configuredPlugin)) {
                    try {
                        matched = true;
                        String latestVersion = pluginManager.getLatestLocalVersion(pvcp.getArtifactGroup(), pvcp.getArtifactName());
                        Version version = versionScheme.parseVersion(latestVersion);
                        if (!pvcp.getArtifactVersion().equals(latestVersion)) {
                            Version previousVersion = versionScheme.parseVersion(pvcp.getArtifactVersion());
                            if (previousVersion.compareTo(version) == -1) {
                                if (!pvcp.isPinVersion()) {
                                    logger.info("Upgrading from {}:{}:{} to {}", pvcp.getArtifactGroup(), pvcp.getArtifactName(),
                                            pvcp.getArtifactVersion(), latestVersion);
                                    pvcp.setArtifactVersion(latestVersion);
                                    pvcp.setLatestArtifactVersion(latestVersion);
                                } else {
                                    logger.info("Not upgrading from {}:{}:{} to {} because the version is pinned", pvcp.getArtifactGroup(),
                                            pvcp.getArtifactName(), pvcp.getArtifactVersion(), latestVersion);
                                    pvcp.setLatestArtifactVersion(latestVersion);
                                }
                                configurationService.save(pvcp);
                            } else {
                                logger.info(
                                        "The latest version in the local repository was older than the configured version.  The configured version was {}:{}:{}.  "
                                                + "The latest version is {}",
                                        pvcp.getArtifactGroup(), pvcp.getArtifactName(), pvcp.getArtifactVersion(), latestVersion);
                            }
                        }

                        load(projectVersionId, pvcp.getArtifactGroup(), pvcp.getArtifactName(), pvcp.getArtifactVersion(),
                                remoteRepostiories);

                    } catch (InvalidVersionSpecificationException e) {
                        logger.error("", e);
                    }
                }
            }

            if (!matched) {
                String latestVersion = pluginManager.getLatestLocalVersion(configuredPlugin.getArtifactGroup(),
                        configuredPlugin.getArtifactName());
                if (latestVersion != null) {
                    String pluginId = load(projectVersionId, configuredPlugin.getArtifactGroup(), configuredPlugin.getArtifactName(),
                            latestVersion, remoteRepostiories);

                    List<XMLComponent> components = componentsByPluginId.get(pluginId);
                    if (components != null) {
                        for (XMLComponent xmlComponent : components) {
                            ProjectVersionComponentPlugin plugin = new ProjectVersionComponentPlugin();
                            plugin.setProjectVersionId(projectVersionId);
                            plugin.setComponentTypeId(xmlComponent.getId());
                            plugin.setComponentName(xmlComponent.getName());
                            plugin.setArtifactGroup(configuredPlugin.getArtifactGroup());
                            plugin.setArtifactName(configuredPlugin.getArtifactName());
                            plugin.setArtifactVersion(latestVersion);
                            plugin.setLatestArtifactVersion(latestVersion);
                            configurationService.save(plugin);
                        }
                    } else {
                        logger.warn("Could not find a component in the {} plugin", pluginId);
                    }

                } else {
                    logger.warn("Could not find a registered plugin for {}:{}", configuredPlugin.getArtifactGroup(),
                            configuredPlugin.getArtifactName());
                }
            }
        }
    }

    @Override
    public List<XMLComponent> getDefinitions(String projectVersionId) {
        return new ArrayList<>(componentsByProjectVersionIdById.get(projectVersionId).values());
    }

    protected String load(String projectVersionId, String artifactGroup, String artifactName, String artifactVersion,
            List<PluginRepository> pluginRepository) {
        ClassLoader classLoader = pluginManager.getClassLoader(artifactGroup, artifactName, artifactVersion, pluginRepository);
        String pluginId = pluginManager.toPluginId(artifactGroup, artifactName, artifactVersion);
        if (classLoader != null) {
            loadComponentsForClassloader(projectVersionId, pluginId, classLoader);
        } else {
            logger.warn("Could not find plugin with the id of {}", pluginId);
        }
        return pluginId;
    }

    @Override
    synchronized public XMLComponent getDefinition(String projectVersionId, String id) {
        Map<String, XMLComponent> componentsById = componentsByProjectVersionIdById.get(projectVersionId);
        if (componentsById != null) {
            return componentsById.get(id);
        } else {
            logger.warn("Could not find components for project version of {}", projectVersionId);
            return null;
        }
    }

    protected void reset() {
        componentsByPluginId = new HashMap<>();
        componentsByProjectVersionIdById = new HashMap<>();
    }

    protected void loadComponentsForClassloader(String projectVersionId, String pluginId, ClassLoader classLoader) {
        try {
            JAXBContext jc = JAXBContext.newInstance(XMLComponents.class.getPackage().getName());
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            List<InputStream> componentXmls = loadResources("components.xml", classLoader);
            Map<String, XMLComponent> componentsById = componentsByProjectVersionIdById.get(projectVersionId);
            if (componentsById == null) {
                componentsById = new HashMap<>();
                componentsByProjectVersionIdById.put(projectVersionId, componentsById);
            }
            try {
                for (InputStream inputStream : componentXmls) {
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    @SuppressWarnings("unchecked")
                    JAXBElement<XMLComponents> root = (JAXBElement<XMLComponents>) unmarshaller.unmarshal(reader);
                    XMLComponents components = root.getValue();
                    List<XMLComponent> componentList = components.getComponent();
                    for (XMLComponent xmlComponent : componentList) {
                        String id = xmlComponent.getId();
                        List<XMLComponent> componentsForPluginId = componentsByPluginId.get(pluginId);
                        if (componentsForPluginId == null) {
                            componentsForPluginId = new ArrayList<>();
                            componentsByPluginId.put(pluginId, componentsForPluginId);
                        }
                        componentsForPluginId.add(xmlComponent);

                        if (!componentsById.containsKey(id)) {
                            xmlComponent.setClassLoader(classLoader);
                            componentsById.put(id, xmlComponent);
                            logger.debug("Registering '{}' with an id of '{}' for plugin '{}' for project '{}'", xmlComponent.getName(), id,
                                    pluginId, projectVersionId);

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
                            xmlComponent.getSettings().getSetting().add(new XMLSetting(DESCRIPTION, "Description", null, Type.MULTILINE_TEXT, false));
                        } else {
                            if (!classLoader.equals(componentsById.get(id).getClassLoader())) {
                                logger.debug(
                                        "There was already a component registered under the id of '{}' with the name '{}' from another plugin.  Not loading it for the plugin '{}'",
                                        new Object[] { id, xmlComponent.getName(), pluginId });
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
