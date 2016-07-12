package org.jumpmind.metl.core.plugin;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jumpmind.metl.core.model.PluginArtifact;
import org.jumpmind.metl.core.model.PluginType;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements IPluginManager {

    final static Logger logger = LoggerFactory.getLogger(PluginManager.class);
    
    String localRepositoryPath;

    IConfigurationService configurationService;

    RepositorySystem repositorySystem;

    RepositorySystemSession repositorySystemSession;

    public PluginManager(IConfigurationService configurationService, String localRepositoryPath) {
        this.configurationService = configurationService;
        this.localRepositoryPath = localRepositoryPath;
        repositorySystem = newRepositorySystem();
        repositorySystemSession = newRepositorySystemSession(repositorySystem, localRepositoryPath);
        List<PluginArtifact> outOfTheBoxPlugins = getOutOfTheBoxPlugins();
        for (PluginArtifact pluginArtifact : outOfTheBoxPlugins) {
            configurationService.save(pluginArtifact);
        }
    }
    
    protected List<PluginArtifact> getOutOfTheBoxPlugins() {
        List<PluginArtifact> artifacts = new ArrayList<>();
        artifacts.add(new PluginArtifact("comp-rdbms-reader", "org.jumpmind.metl", PluginType.COMPONENT));
        return artifacts;
    }

    public void addRepository(String url) {

    }

    public ClassLoader load(String jarDependency) {
        return null;
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