package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.AgentSummary;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersionSummary;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.config.Model;
import org.jumpmind.symmetric.is.core.config.ModelAttribute;
import org.jumpmind.symmetric.is.core.config.ModelAttributeRelationship;
import org.jumpmind.symmetric.is.core.config.ModelEntity;
import org.jumpmind.symmetric.is.core.config.ModelEntityRelationship;
import org.jumpmind.symmetric.is.core.config.ModelVersion;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;

public interface IConfigurationService {

    public List<Folder> findFolders(FolderType type);

    public void deleteFolder(String folderId);

    public void delete(Agent agent);
    
    public void delete(AgentDeployment agentDeployment);
    
    public void delete(ComponentFlow flow);
    
    public void delete(ComponentFlowVersion flow, ComponentFlowNode flowNode);
    
    public void delete(ComponentFlowNodeLink link);
    
    public void delete(Connection connection);
    
    public boolean isDeployed(ComponentFlowVersion componentFlowVersion);
    
    public boolean isDeployed(ComponentFlow componentFlow);
    
    public List<ComponentFlow> findComponentFlowsInFolder(Folder folder);
    
    public Connection findConnection(String id);
    
    public List<Connection> findConnectionsInFolder(Folder folder);
    
    public List<Connection> findConnectionsByTypes(String ... types);
    
    public List<Agent> findAgentsInFolder(Folder folder);
    
    public List<Agent> findAgentsForHost(String hostName);
    
    public List<AgentDeployment> findAgentDeploymentsFor(ComponentFlowVersion componentFlowVersion);
    
    public void deleteComponentFlowVersion(ComponentFlowVersion componentFlowVersion);

    public void refresh(ComponentFlowVersion componentFlowVersion);
    
    public void refresh(Agent agent);
    
    public void refresh(Connection connection);
    
    public void save(Connection connection);
    
    public void save(ComponentFlowNode flowNode);

    public abstract void save(AbstractObject<?> obj);
    
    public abstract void save(AbstractData obj);

    public void save(ComponentFlowVersion componentFlowVersion);
    
    public List<ComponentFlowVersionSummary> findUndeployedComponentFlowVersionSummary(String agentId);
    
    public List<AgentSummary> findUndeployedAgentsFor(String componentFlowVersionId);
    
    public void delete(Model model);
    
    public void delete(ModelVersion modelVersion);
    
    public void delete(ModelEntity modelEntity);
    
    public void delete(ModelAttribute modelAttribute);
    
    public void delete(ModelEntityRelationship modelEntityRelationship);
    
    public void delete(ModelAttributeRelationship modelAttributeRelationship);
    
    public void refresh(ModelVersion modelVersion);
    
    public void refresh(ModelEntity modelEntity);
    
    public void refresh(ModelAttribute modelAttribute);
    
    public void refresh(ModelEntityRelationship modelEntityRelationship);
    
    public void refresh(ModelAttributeRelationship modelAttributeRelationship);
    
    public void save(ModelVersion modelVersion);
    
    public void save(ModelEntity modelEntity);
    
    public void save(ModelAttribute modelAttribute);
    
    public void save(ModelEntityRelationship modelEntityRelationship);
    
    public void save(ModelAttributeRelationship modelAttributeRelationship);

}
