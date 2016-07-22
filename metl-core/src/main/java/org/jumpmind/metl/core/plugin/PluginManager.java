package org.jumpmind.metl.core.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements IPluginManager {

    final static Logger logger = LoggerFactory.getLogger(PluginManager.class);

    String localRepositoryPath;

    RepositorySystem repositorySystem;

    RepositorySystemSession repositorySystemSession;

    public PluginManager(String localRepositoryPath) {
        this.localRepositoryPath = localRepositoryPath;
    }

    @Override
    public void init() {
        repositorySystem = newRepositorySystem();
        repositorySystemSession = newRepositorySystemSession(repositorySystem, localRepositoryPath);
    }

    @Override
    public ClassLoader getClassLoader(String artifactGroup, String artifactName, String artifactVersion) {
        try {
            Artifact artifact = new DefaultArtifact(String.format("%s:%s:%s", artifactGroup, artifactName, artifactVersion));
            DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));

            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

            List<ArtifactResult> artifactResults = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest)
                    .getArtifactResults();

            List<URL> artifactUrls = new ArrayList<URL>();
            for (ArtifactResult artRes : artifactResults) {
                artifactUrls.add(artRes.getArtifact().getFile().toURI().toURL());
            }

            return new URLClassLoader(artifactUrls.toArray(new URL[artifactUrls.size()]), getClass().getClassLoader());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void deploy (String artifactGroup, String artifactName, String artifactVersion, String filePath) {
//        Artifact jarArtifact = new DefaultArtifact(artifactGroup, artifactName, "jar", artifactVersion);
//        jarArtifact = jarArtifact.setFile(new File( filePath ));
//
//        RemoteRepository distRepo =
//            new RemoteRepository.Builder( "org.eclipse.aether.examples", "default",
//                                  new File( "target/dist-repo" ).toURI().toString() ).build();
//
//        DeployRequest deployRequest = new DeployRequest();
//        deployRequest.addArtifact( jarArtifact );
//        deployRequest.setRepository( distRepo );
//
//        repositorySystem.deploy( repositorySystemSession, deployRequest );
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

    @Override
    synchronized public void reload() {
        // plugins = new HashMap<>();
        // List<PluginArtifact> list =
        // configurationService.findPluginArtifacts();
        // for (PluginArtifact pluginArtifact : list) {
        // List<PluginArtifactVersion> versions =
        // pluginArtifact.getPluginArtifactVersions();
        // for (PluginArtifactVersion pluginArtifactVersion : versions) {
        // try {
        // Artifact artifact = new
        // DefaultArtifact(pluginArtifactVersion.getId());
        // DependencyFilter classpathFlter =
        // DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        //
        // CollectRequest collectRequest = new CollectRequest();
        // collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
        // List<RemoteRepository> remoteRepositories = Arrays
        // .asList(new RemoteRepository.Builder("jumpmind", "default",
        // "http://maven.jumpmind.com/repo").build());
        // collectRequest.setRepositories(remoteRepositories);
        //
        // DependencyRequest dependencyRequest = new
        // DependencyRequest(collectRequest, classpathFlter);
        //
        // List<ArtifactResult> artifactResults =
        // repositorySystem.resolveDependencies(repositorySystemSession,
        // dependencyRequest)
        // .getArtifactResults();
        //
        // List<URL> artifactUrls = new ArrayList<URL>();
        // for (ArtifactResult artRes : artifactResults) {
        // artifactUrls.add(artRes.getArtifact().getFile().toURI().toURL());
        // }
        // plugins.put(pluginArtifactVersion,
        // new URLClassLoader(artifactUrls.toArray(new
        // URL[artifactUrls.size()]), getClass().getClassLoader()));
        // } catch (Exception e) {
        // logger.error("Failed to resolve dependency: " +
        // pluginArtifactVersion.getId(), e);
        // }
        // }
        // }
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
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

}