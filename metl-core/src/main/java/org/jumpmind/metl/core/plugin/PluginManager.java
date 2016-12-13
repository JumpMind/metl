package org.jumpmind.metl.core.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.classpath.ClasspathTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.h2.store.fs.FileUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.util.ChildFirstURLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements IPluginManager {

    final static Logger logger = LoggerFactory.getLogger(PluginManager.class);

    List<Plugin> outOfTheBox = new ArrayList<>();

    String localRepositoryPath;

    RepositorySystem repositorySystem;

    RepositorySystemSession repositorySystemSession;

    Map<String, ClassLoader> plugins = new HashMap<>();

    IConfigurationService configurationService;

    public PluginManager(String localRepositoryPath, IConfigurationService configurationService) {
        this.localRepositoryPath = localRepositoryPath;
        this.configurationService = configurationService;
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-rdbms-reader", 10));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-rdbms-writer", 20));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-data-diff", 30));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-sorter", 40));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-temp-rdbms", 50));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-serialize", 60));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-service", 70));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-sequence", 80));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-sql-execute", 90));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-zip", 100));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-file", 110));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-excel", 120));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-control", 130));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-groovy", 140));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-route", 150));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-map", 160));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-transform", 170));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-xml", 180));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-format", 190));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-email", 200));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-execute", 210));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-webrequest", 220));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-test", 230));
        outOfTheBox.add(new Plugin("org.jumpmind.metl", "comp-text", 240));
    }

    @Override
    public void init() {
        logger.info("Initializing plugin manager.  The local repository is located at: " + localRepositoryPath);
        repositorySystem = newRepositorySystem();
        repositorySystemSession = newRepositorySystemSession(repositorySystem, localRepositoryPath);
    }

    @Override
    public List<Plugin> getOutOfTheBox() {
        return outOfTheBox;
    }

    @Override
    public boolean isNewer(Plugin first, Plugin second) {
        try {
            GenericVersionScheme versionScheme = new GenericVersionScheme();
            Version firstVersion = versionScheme.parseVersion(first.getArtifactVersion());
            Version secondVersion = versionScheme.parseVersion(second.getArtifactVersion());
            return firstVersion.compareTo(secondVersion) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void refresh() {
        plugins = new HashMap<>();
        checkForNewOutOfTheBoxVersions();
        checkForNewConfiguredVersions();
        loadAll();
    }

    protected void loadAll() {
        List<Plugin> existing = configurationService.findPlugins();
        List<PluginRepository> repositories = configurationService.findPluginRepositories();
        for (Plugin plugin : existing) {
            try {
                getClassLoader(plugin.getArtifactGroup(), plugin.getArtifactName(), plugin.getArtifactVersion(), repositories);
            } catch (Exception ex) {
                logger.warn("Failed to load plugin: %s", plugin);
                logger.debug("", ex);
            }
        }
    }

    @Override
    public List<String> getAvailableVersions(String artifactGroup, String artifactName, List<PluginRepository> remoteRepositories) {
        List<String> versions = new ArrayList<>();
        try {
            VersionRangeRequest rangeRequest = new VersionRangeRequest();
            rangeRequest.setArtifact(new DefaultArtifact(artifactGroup, artifactName, "jar", "[0,)"));
            if (remoteRepositories != null) {
                for (PluginRepository pluginRepository : remoteRepositories) {
                    rangeRequest.addRepository(
                            new RemoteRepository.Builder(pluginRepository.getName(), "default", pluginRepository.getUrl()).build());
                }
            }
            VersionRangeResult rangeResult = repositorySystem.resolveVersionRange(repositorySystemSession, rangeRequest);
            if (rangeResult != null) {
                List<Version> versionList = rangeResult.getVersions();
                for (Version version : versionList) {
                    versions.add(version.toString());
                }
            }
        } catch (VersionRangeResolutionException e) {
            logger.error("", e);
        }
        return versions;
    }

    protected void checkForNewOutOfTheBoxVersions() {
        checkForNewerVersion(outOfTheBox);
    }

    protected void checkForNewConfiguredVersions() {
        checkForNewerVersion(configurationService.findActivePlugins());
    }

    protected void checkForNewerVersion(List<Plugin> listToCheck) {
        Set<String> checked = new HashSet<>();
        for (Plugin plugin : listToCheck) {
            String id = String.format("%s:%s", plugin.getArtifactGroup(), plugin.getArtifactName());
            if (!checked.contains(id)) {
                List<Plugin> existing = configurationService.findPlugins();
                String latestVersion = getLatestLocalVersion(plugin.getArtifactGroup(), plugin.getArtifactName());
                if (latestVersion != null) {
                    Plugin potentialNewVersion = new Plugin(plugin.getArtifactGroup(), plugin.getArtifactName(), latestVersion,
                            plugin.getLoadOrder());

                    boolean matched = false;
                    for (Plugin existingPlugin : existing) {
                        if (existingPlugin.equals(potentialNewVersion)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        logger.info("Found a new version of {}.  Recording it", potentialNewVersion);
                        configurationService.save(potentialNewVersion);
                    }
                    checked.add(id);
                }
            }
        }
    }

    protected boolean isOutOfTheBox(Plugin plugin) {
        boolean matched = false;
        for (Plugin ootbp : outOfTheBox) {
            if (plugin.matches(ootbp.getArtifactGroup(), ootbp.getArtifactName())) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    @Override
    public String toPluginId(String artifactGroup, String artifactName, String artifactVersion) {
        return String.format("%s:%s:%s", artifactGroup, artifactName, artifactVersion);
    }

    @Override
    public void delete(String artifactGroup, String artifactName, String artifactVersion) {
        String pluginId = toPluginId(artifactGroup, artifactName, artifactVersion);
        Artifact artifact = new DefaultArtifact(pluginId);
        File file = new File(localRepositoryPath, repositorySystemSession.getLocalRepositoryManager().getPathForLocalArtifact(artifact));
        File dir = file.getParentFile();
        if (dir.exists() && dir.isDirectory()) {
            logger.info("Attempting to delete {} at {}", pluginId, dir);
            FileUtils.deleteRecursive(dir.getPath(), true);
        }
    }

    @Override
    public ClassLoader getClassLoader(String artifactGroup, String artifactName, String artifactVersion,
            List<PluginRepository> remoteRepositories) {
        String pluginId = toPluginId(artifactGroup, artifactName, artifactVersion);
        ClassLoader classLoader = plugins.get(pluginId);
        if (classLoader == null) {
            try {
                Artifact artifact = new DefaultArtifact(pluginId);

                List<ArtifactResult> artifactResults = new ArrayList<>();
                artifactResults.addAll(collectDependencies(artifact, JavaScopes.COMPILE, remoteRepositories));
                artifactResults.addAll(collectDependencies(artifact, JavaScopes.RUNTIME, remoteRepositories));

                List<URL> artifactUrls = new ArrayList<URL>();
                for (ArtifactResult artRes : artifactResults) {
                    URL url = artRes.getArtifact().getFile().toURI().toURL();
                    if (!artifactUrls.contains(url)) {
                        artifactUrls.add(0, url);
                    }
                }

                classLoader = new ChildFirstURLClassLoader(artifactUrls.toArray(new URL[artifactUrls.size()]), getClass().getClassLoader());
                plugins.put(pluginId, classLoader);
            } catch (Exception e) {
                logger.warn("Failed to get class loader for {}:{}:{}", artifactGroup, artifactName, artifactVersion);
                logger.debug("", e);
            }
        }
        return classLoader;
    }

    protected List<ArtifactResult> merge(List<ArtifactResult>[] results) {
        List<ArtifactResult> merged = new ArrayList<>();
        for (List<ArtifactResult> artifactResults : results) {
            for (ArtifactResult artifactResult : artifactResults) {
                if (!merged.contains(artifactResult)) {
                    merged.add(artifactResult);
                }
            }
        }
        return merged;
    }

    protected List<ArtifactResult> collectDependencies(Artifact artifact, String scope, List<PluginRepository> remoteRepositories) {
        try {
            DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(scope);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, scope));

            for (PluginRepository pluginRepository : remoteRepositories) {
                collectRequest.addRepository(
                        new RemoteRepository.Builder(pluginRepository.getName(), "default", pluginRepository.getUrl()).build());
            }

            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

            return repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest).getArtifactResults();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void install(String groupId, String artifactId, String version, File file) {
        try {
            Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, "", "jar", version).setFile(file);
            Model model = new Model();
            model.setArtifactId(artifactId);
            model.setGroupId(groupId);
            model.setVersion(version);
            model.setModelVersion("4.0.0");
            File pomFile = File.createTempFile("pom", ".xml");
            new DefaultModelWriter().write(pomFile, null, model);
            InstallRequest request = new InstallRequest();
            request.addArtifact(new DefaultArtifact(groupId, artifactId, null, "pom", version, null, pomFile));
            request.addArtifact(jarArtifact);
            repositorySystem.install(repositorySystemSession, request);
            pomFile.delete();
            Plugin newVersion = new Plugin(groupId, artifactId, version, 0);
            configurationService.save(newVersion);
        } catch (IOException e) {
            throw new IoException(e);
        } catch (InstallationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getLatestLocalVersion(String artifactGroup, String artifactName) {
        String latestVersion = null;
        try {
            VersionRangeRequest rangeRequest = new VersionRangeRequest();
            rangeRequest.setArtifact(new DefaultArtifact(artifactGroup, artifactName, "jar", "[0,)"));
            VersionRangeResult rangeResult = repositorySystem.resolveVersionRange(repositorySystemSession, rangeRequest);
            if (rangeResult != null && rangeResult.getHighestVersion() != null) {
                latestVersion = rangeResult.getHighestVersion().toString();
            }
        } catch (VersionRangeResolutionException e) {
            logger.error("", e);
        }
        return latestVersion;
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, ClasspathTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                logger.error("", exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem repositorySystem, String localRepositoryPath) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepositoryPath);
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));
        session.setTransferListener(new PluginTransferListener());
        session.setRepositoryListener(new PluginRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

}