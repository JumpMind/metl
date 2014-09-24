package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.app.core.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentGraph;
import org.jumpmind.symmetric.is.core.config.ComponentGraphVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.StructuredModel;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public class ConfigurationService {

    protected IPersistenceManager persistenceManager;

    public void save(ComponentGraphVersion graph) {
    }

    public void save(ComponentVersion component) {
    }

    public void save(Component component) {
    }

    public void save(ComponentGraph component) {
    }
    
    public void save(Folder folder) {}

    public List<Folder> findFolders(FolderType type) {
        return null;
    }

    public List<Component> findComponents(Folder folder) {
        return null;
    }

    public List<ComponentGraph> findComponentGraphs(Folder folder) {
        return null;
    }

    public List<StructuredModel> findStructuredModel(Folder folder) {
        return null;
    }

    public List<Connection> findConnections(Folder foler) {
        return null;
    }

    public List<Agent> findAgents(Folder folder) {
        return null;
    }

    public List<AgentDeployment> findAgentDeployments(Agent agent) {
        return null;
    }

}
