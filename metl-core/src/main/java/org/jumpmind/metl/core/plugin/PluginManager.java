package org.jumpmind.metl.core.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.jumpmind.metl.core.model.PluginArtifact;
import org.jumpmind.metl.core.model.PluginArtifactVersion;
import org.jumpmind.metl.core.model.PluginType;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements IPluginManager {

    final static Logger logger = LoggerFactory.getLogger(PluginManager.class);

    String localRepositoryPath;

    IConfigurationService configurationService;

    RepositorySystem repositorySystem;

    RepositorySystemSession repositorySystemSession;

    Map<PluginArtifactVersion, ClassLoader> plugins;

    public PluginManager(IConfigurationService configurationService, String localRepositoryPath) {
        this.configurationService = configurationService;
        this.localRepositoryPath = localRepositoryPath;
    }

    public void init() {
        repositorySystem = newRepositorySystem();
        repositorySystemSession = newRepositorySystemSession(repositorySystem, localRepositoryPath);
        List<PluginArtifact> outOfTheBoxPlugins = getOutOfTheBoxPlugins();
        for (PluginArtifact pluginArtifact : outOfTheBoxPlugins) {
            configurationService.save(pluginArtifact);
        }
    }

    @Override
    synchronized public Map<PluginArtifactVersion, ClassLoader> getPlugins() {
        if (plugins == null) {
            reload();
        }
        return plugins;
    }

    @Override
    synchronized public void reload() {
        plugins = new HashMap<>();
        List<PluginArtifact> list = configurationService.findPluginArtifacts();
        for (PluginArtifact pluginArtifact : list) {
            List<PluginArtifactVersion> versions = pluginArtifact.getPluginArtifactVersions();
            for (PluginArtifactVersion pluginArtifactVersion : versions) {
                try {
                    Artifact artifact = new DefaultArtifact(pluginArtifactVersion.getId());
                    DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

                    CollectRequest collectRequest = new CollectRequest();
                    collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
                    List<RemoteRepository> remoteRepositories = Arrays
                            .asList(new RemoteRepository.Builder("jumpmind", "default", "http://maven.jumpmind.com/repo").build());
                    collectRequest.setRepositories(remoteRepositories);

                    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

                    List<ArtifactResult> artifactResults = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest)
                            .getArtifactResults();

                    List<URL> artifactUrls = new ArrayList<URL>();
                    for (ArtifactResult artRes : artifactResults) {
                        artifactUrls.add(artRes.getArtifact().getFile().toURI().toURL());
                    }
                    plugins.put(pluginArtifactVersion,
                            new URLClassLoader(artifactUrls.toArray(new URL[artifactUrls.size()]), getClass().getClassLoader()));
                } catch (Exception e) {
                    logger.error("Failed to resolve dependency: " + pluginArtifactVersion.getId(), e);
                }
            }
        }
    }

    protected List<PluginArtifact> getOutOfTheBoxPlugins() {
        List<PluginArtifact> artifacts = new ArrayList<>();
        artifacts.add(new PluginArtifact("comp-rdbms-reader", "org.jumpmind.metl", PluginType.COMPONENT, VersionUtils.getCurrentVersion()));
        return artifacts;
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

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

        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

}