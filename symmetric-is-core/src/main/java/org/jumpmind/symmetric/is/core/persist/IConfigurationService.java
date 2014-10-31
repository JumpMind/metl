package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersionSummary;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;

public interface IConfigurationService {

    public List<Folder> findFolders(FolderType type);

    public void deleteFolder(String folderId);

    public void delete(Agent agent);
    
    public void delete(ComponentFlow flow);
    
    public void delete(ComponentFlowNode flowNode);
    
    public void delete(ComponentFlowNodeLink link);
    
    public void delete(Connection connection);
    
    public List<ComponentFlow> findComponentFlowsInFolder(Folder folder);
    
    public List<Connection> findConnectionsInFolder(Folder folder);
    
    public List<Agent> findAgentsInFolder(Folder folder);
    
    public List<Agent> findAgentsForHost(String hostName);
    
    public void deleteComponentFlowVersion(ComponentFlowVersion componentFlowVersion);

    public void refresh(ComponentFlowVersion componentFlowVersion);
    
    public void refresh(Agent agent);
    
    public void refresh(Connection connection);
    
    public void save(Connection connection);

    public abstract void save(AbstractObject<?> obj);

    public void save(ComponentFlowVersion componentFlowVersion);
    
    public List<ComponentFlowVersionSummary> findUndeployedComponentFlowVersionSummary(String agentId);
    
}
