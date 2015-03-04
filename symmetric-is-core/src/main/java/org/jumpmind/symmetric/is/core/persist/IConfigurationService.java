package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentSummary;
import org.jumpmind.symmetric.is.core.model.ComponentFlow;
import org.jumpmind.symmetric.is.core.model.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.model.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.model.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.model.ComponentFlowVersionSummary;
import org.jumpmind.symmetric.is.core.model.Connection;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelAttributeRelationship;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.ModelEntityRelationship;
import org.jumpmind.symmetric.is.core.model.ModelVersion;

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
    
    public List<Model> findModelsInFolder(Folder folder);
    
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

    public void save(AbstractObject obj);

    public void save(ComponentFlowVersion componentFlowVersion);
    
    public List<ComponentFlowVersionSummary> findUndeployedComponentFlowVersionSummary(String agentId);
    
    public List<AgentSummary> findUndeployedAgentsFor(String componentFlowVersionId);
    
    public void delete(Model model);
    
    public void delete(ModelVersion modelVersion);
    
    public void delete(ModelEntity modelEntity);
    
    public void delete(ModelAttribute modelAttribute);
    
    public void delete(ModelEntityRelationship modelEntityRelationship);
    
    public void delete(ModelAttributeRelationship modelAttributeRelationship);
    
    public void refresh(Model model);
    
    public void refresh(ModelVersion modelVersion);
    
    public void refresh(ModelEntity modelEntity);
    
    public void refresh(ModelAttribute modelAttribute);
    
    public void refresh(ModelEntityRelationship modelEntityRelationship);
    
    public void refresh(ModelAttributeRelationship modelAttributeRelationship);
    
    public void save(ModelVersion modelVersion);
    
    public void save(ModelEntity modelEntity);
    
    public void save(ModelEntityRelationship modelEntityRelationship);

}
