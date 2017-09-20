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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jumpmind.metl.core.plugin.PluginConstants.DEFINTION_TYPE_COMPONENT;
import static org.jumpmind.metl.core.plugin.PluginConstants.DEFINTION_TYPE_RESOURCE;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.ENABLED;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.INBOUND_QUEUE_CAPACITY;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.LOG_INPUT;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.LOG_OUTPUT;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.NOTES;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.jumpmind.metl.core.model.ProjectVersionPlugin;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IPluginService;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;
import org.jumpmind.metl.core.plugin.XMLSetting.Type;
import org.jumpmind.metl.core.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefinitionFactory implements IDefinitionFactory {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Map<String, Map<String, XMLAbstractDefinition>> definitionsByProjectVersionIdById;

    protected Map<String, List<XMLAbstractDefinition>> definitionsByPluginId;

    protected IConfigurationService configurationService;
    
    protected IPluginService pluginService;

    protected IPluginManager pluginManager;

    protected JAXBContext xmlContext;

    public DefinitionFactory() {
        definitionsByProjectVersionIdById = new HashMap<>();
        definitionsByPluginId = new HashMap<>();
    }

    public DefinitionFactory(IPluginService pluginService, IConfigurationService configurationService, IPluginManager pluginManager) {
        this();
        this.configurationService = configurationService;
        this.pluginManager = pluginManager;
        this.pluginService = pluginService;
    }

    @Override
    public void refresh() {
        long ts = System.currentTimeMillis();
        definitionsByProjectVersionIdById = new HashMap<>();
        definitionsByPluginId = new HashMap<>();
        if (pluginManager != null && configurationService != null) {
            pluginManager.refresh();
            List<Plugin> distinctPlugins = pluginService.findDistinctPlugins();
            List<String> projectVersionIds = configurationService.findAllProjectVersionIds();
            int numOfVersions = projectVersionIds.size(); 
            if (numOfVersions > 0) {
                int numOfExecutors = numOfVersions > 10 ? numOfVersions/2 : numOfVersions;
                ExecutorService executor = Executors.newFixedThreadPool(numOfExecutors, new RefreshThreadFactory());
                List<Future<?>> futures = new ArrayList<Future<?>>();
                for (String projectVersionId : projectVersionIds) {
                    futures.add(executor.submit(() -> refresh(projectVersionId, distinctPlugins)));
                }
                awaitTermination(executor, futures);
            }
            logger.info("It took {}ms to refresh {} project versions", (System.currentTimeMillis()-ts), numOfVersions);
        }
    }

    @Override
    public void refresh(String projectVersionId) {
        List<Plugin> distinctPlugins = pluginService.findDistinctPlugins();
        refresh(projectVersionId, distinctPlugins);
    }        
        
    protected void refresh(String projectVersionId, List<Plugin> distinctPlugins) {
        long ts = System.currentTimeMillis();
        loadComponentsForClassloader(projectVersionId, "org.jumpmind.metl:metl-core:" + VersionUtils.getCurrentVersion(),
                getClass().getClassLoader());
        List<PluginRepository> remoteRepostiories = pluginService.findPluginRepositories();
        List<ProjectVersionPlugin> pvcps = configurationService.findProjectVersionComponentPlugins(projectVersionId);
        GenericVersionScheme versionScheme = new GenericVersionScheme();        
        for (Plugin configuredPlugin : distinctPlugins) {
            boolean matched = false;
            for (ProjectVersionPlugin pvcp : pvcps) {
                if (pvcp.matches(configuredPlugin)) {
                    try {
                        matched = true;
                        String latestVersion = pluginManager.getLatestLocalVersion(pvcp.getArtifactGroup(), pvcp.getArtifactName());
                        if (isNotBlank(latestVersion)) {
                            Version version = versionScheme.parseVersion(latestVersion);
                            if (!pvcp.getArtifactVersion().equals(latestVersion)) {
                                Version previousVersion = versionScheme.parseVersion(pvcp.getArtifactVersion());
                                if (previousVersion.compareTo(version) == -1) {
                                    if (!pvcp.isPinVersion()) {
                                        logger.info("Upgrading {}:{} from {} to {}", pvcp.getArtifactGroup(), pvcp.getArtifactName(),
                                                pvcp.getArtifactVersion(), latestVersion);
                                        pvcp.setArtifactVersion(latestVersion);
                                        pvcp.setLatestArtifactVersion(latestVersion);
                                    } else {
                                        logger.info("Not upgrading {}:{} from {} to {} because the version is pinned",
                                                pvcp.getArtifactGroup(), pvcp.getArtifactName(), pvcp.getArtifactVersion(), latestVersion);
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
                        }
                        
                        matched = null != load(projectVersionId, pvcp.getArtifactGroup(), pvcp.getArtifactName(), pvcp.getArtifactVersion(),
                                remoteRepostiories);
                        
                        if (!matched) {
                            logger.warn("Deleting the reference to {}:{}:{}", pvcp.getArtifactGroup(), pvcp.getArtifactName(), pvcp.getArtifactVersion());
                            pluginService.delete(pvcp);
                            pluginService.delete((Plugin)pvcp);
                        }                        

                    } catch (InvalidVersionSpecificationException e) {
                        logger.error("", e);
                    } 
                    
                    break;
                }
            }

            if (!matched) {
                String latestVersion = pluginManager.getLatestLocalVersion(configuredPlugin.getArtifactGroup(),
                        configuredPlugin.getArtifactName());
                if (latestVersion != null) {
                    String pluginId = load(projectVersionId, configuredPlugin.getArtifactGroup(), configuredPlugin.getArtifactName(),
                            latestVersion, remoteRepostiories);

                    List<XMLAbstractDefinition> definitions = definitionsByPluginId.get(pluginId);
                    if (definitions != null) {
                        definitions = new ArrayList<>(definitionsByPluginId.get(pluginId));
                        for (XMLAbstractDefinition definition : definitions) {
                            ProjectVersionPlugin plugin = new ProjectVersionPlugin();
                            plugin.setProjectVersionId(projectVersionId);
                            plugin.setDefinitionTypeId(definition.getId());
                            plugin.setDefinitionName(definition.getName());
                            plugin.setArtifactGroup(configuredPlugin.getArtifactGroup());
                            plugin.setArtifactName(configuredPlugin.getArtifactName());
                            plugin.setArtifactVersion(latestVersion);
                            plugin.setLatestArtifactVersion(latestVersion);
                            if (definition instanceof XMLComponentDefinition) {
                                plugin.setDefinitionType(DEFINTION_TYPE_COMPONENT);
                            } else if (definition instanceof XMLResourceDefinition) {
                                plugin.setDefinitionType(DEFINTION_TYPE_RESOURCE);
                            } else {
                                throw new IllegalStateException("Unknown definition type");
                            }
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
        long time = System.currentTimeMillis()-ts;
        if (time > 1000) {
            logger.info("It took {}ms to refresh plugins for project version: {}", time, projectVersionId);
        }
    }

    @Override
    public List<XMLComponentDefinition> getComponentDefinitions(String projectVersionId) {
        List<XMLComponentDefinition> components = new ArrayList<>();
        Collection<XMLAbstractDefinition> definitions = definitionsByProjectVersionIdById.get(projectVersionId).values();
        for (XMLAbstractDefinition xmlAbstractDefinition : definitions) {
            if (xmlAbstractDefinition instanceof XMLComponentDefinition) {
                components.add((XMLComponentDefinition) xmlAbstractDefinition);
            }
        }
        return components;
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
    synchronized public XMLComponentDefinition getComponentDefinition(String projectVersionId, String id) {
        XMLComponentDefinition defintion = null;
        Map<String, XMLAbstractDefinition> definitionsById = definitionsByProjectVersionIdById.get(projectVersionId);
        if (definitionsById != null) {
            XMLAbstractDefinition component = definitionsById.get(id);
            if (component instanceof XMLComponentDefinition) {
                defintion = (XMLComponentDefinition) component;
            }
        }

        if (defintion == null) {
            logger.warn("Could not find components for project version of {} with a type id of {}", projectVersionId, id);
        }

        return defintion;
    }

    @Override
    public XMLResourceDefinition getResourceDefintion(String projectVersionId, String id) {
        XMLResourceDefinition defintion = null;
        Map<String, XMLAbstractDefinition> definitionsById = definitionsByProjectVersionIdById.get(projectVersionId);
        if (definitionsById != null) {
            XMLAbstractDefinition component = definitionsById.get(id);
            if (component instanceof XMLResourceDefinition) {
                defintion = (XMLResourceDefinition) component;
            }
        }

        if (defintion == null) {
            logger.warn("Could not find resource for project version of {} with a type id of {}", projectVersionId, id);
        }

        return defintion;
    }

    @Override
    public Set<XMLResourceDefinition> getResourceDefinitions(String projectVersionId, ResourceCategory resourceCategory) {
        Set<XMLResourceDefinition> categories = new TreeSet<>();
        Map<String, XMLAbstractDefinition> componentsById = definitionsByProjectVersionIdById.get(projectVersionId);
        if (componentsById != null) {
            for (XMLAbstractDefinition definition : componentsById.values()) {
                if (definition instanceof XMLResourceDefinition) {
                    XMLResourceDefinition resource = (XMLResourceDefinition) definition;
                    if (resourceCategory == null || resourceCategory.equals(ResourceCategory.ANY) || 
                            resourceCategory.equals(resource.getResourceCategory())) {
                        categories.add(resource);
                    }
                }
            }
        }
        return categories;
    }

    @Override
    public Set<String> getResourceCategories(String projectVersionId) {
        Set<String> categories = new TreeSet<>();
        Map<String, XMLAbstractDefinition> componentsById = definitionsByProjectVersionIdById.get(projectVersionId);
        if (componentsById != null) {
            for (XMLAbstractDefinition definition : componentsById.values()) {
                if (definition instanceof XMLResourceDefinition) {
                    categories.add(((XMLResourceDefinition) definition).getResourceCategory().name());
                }
            }
        }
        return categories;
    }

    protected void reset() {
        definitionsByPluginId = new HashMap<>();
        definitionsByProjectVersionIdById = new HashMap<>();
    }

    protected void loadComponentsForClassloader(String projectVersionId, String pluginId, ClassLoader classLoader) {
        try {
            Map<String, XMLAbstractDefinition> componentsById = definitionsByProjectVersionIdById.get(projectVersionId);
            if (componentsById == null) {
                componentsById = new HashMap<>();
                definitionsByProjectVersionIdById.put(projectVersionId, componentsById);
            }

            if (xmlContext == null) {
                synchronized (this) {
                    if (xmlContext == null) {
                        xmlContext = JAXBContext.newInstance(XMLDefinitions.class, XMLComponentDefinition.class, XMLSetting.class,
                                XMLSettings.class, XMLSettingChoices.class, ObjectFactory.class);
                    }
                }
            }
            Unmarshaller unmarshaller = xmlContext.createUnmarshaller();
            List<InputStream> componentXmls = new ArrayList<>();
            componentXmls.addAll(loadResources("plugin.xml", classLoader));

            try {
                for (InputStream inputStream : componentXmls) {
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    @SuppressWarnings("unchecked")
                    JAXBElement<XMLDefinitions> root = (JAXBElement<XMLDefinitions>) unmarshaller.unmarshal(reader);
                    XMLDefinitions components = root.getValue();
                    List<XMLComponentDefinition> componentList = components.getComponent();
                    for (XMLComponentDefinition xmlComponent : componentList) {
                        String id = xmlComponent.getId();
                        addXMLAbstractDefinition(pluginId, xmlComponent);
                        if (!componentsById.containsKey(id)) {
                            xmlComponent.setClassLoader(classLoader);
                            componentsById.put(id, xmlComponent);
                            logger.debug("Registering component '{}' with an id of '{}' for plugin '{}' for project '{}'",
                                    xmlComponent.getName(), id, pluginId, projectVersionId);

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
                            xmlComponent.getSettings().getSetting().add(new XMLSetting(NOTES, "Notes", null, Type.MULTILINE_TEXT, false));
                        } else {
                            if (!classLoader.equals(componentsById.get(id).getClassLoader())) {
                                logger.debug(
                                        "There was already a component registered under the id of '{}' with the name '{}' from another plugin.  Not loading it for the plugin '{}'",
                                        new Object[] { id, xmlComponent.getName(), pluginId });
                            }
                        }
                    }

                    List<XMLResourceDefinition> resourceList = components.getResource();
                    for (XMLResourceDefinition xmlResource : resourceList) {
                        String id = xmlResource.getId();
                        addXMLAbstractDefinition(pluginId, xmlResource);

                        if (!componentsById.containsKey(id)) {
                            xmlResource.setClassLoader(classLoader);
                            componentsById.put(id, xmlResource);
                            logger.debug("Registering resource '{}' with an id of '{}' for plugin '{}' for project '{}'",
                                    xmlResource.getName(), id, pluginId, projectVersionId);

                            if (xmlResource.getSettings() == null) {
                                xmlResource.setSettings(new XMLSettings());
                            }

                            if (xmlResource.getSettings().getSetting() == null) {
                                xmlResource.getSettings().setSetting(new ArrayList<XMLSetting>());
                            }

                        } else {
                            if (!classLoader.equals(componentsById.get(id).getClassLoader())) {
                                logger.debug(
                                        "There was already a resource registered under the id of '{}' with the name '{}' from another plugin.  Not loading it for the plugin '{}'",
                                        new Object[] { id, xmlResource.getName(), pluginId });
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

    private synchronized final void addXMLAbstractDefinition(String pluginId, XMLAbstractDefinition definition) {
        List<XMLAbstractDefinition> componentsForPluginId = definitionsByPluginId.get(pluginId);
        if (componentsForPluginId == null) {
            componentsForPluginId = new ArrayList<>();
            definitionsByPluginId.put(pluginId, componentsForPluginId);
        }
        componentsForPluginId.add(definition);
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

    protected void awaitTermination(ExecutorService executor, List<Future<?>> futures) {
        executor.shutdown();
        try {
            if (executor.awaitTermination(1, TimeUnit.HOURS)) {
                for (Future<?> future : futures) {
                    if (future.isDone()) {
                        future.get();
                    }
                }
            } else {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
            throw new RuntimeException(e);
        }
    }

    class RefreshThreadFactory implements ThreadFactory {
        AtomicInteger threadNumber = new AtomicInteger(1);

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("refresh-plugins-" + threadNumber.getAndIncrement());
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }
}
