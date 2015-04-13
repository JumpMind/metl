package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentSummary;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelAttributeRelationship;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.ModelEntityRelationship;
import org.jumpmind.symmetric.is.core.model.Project;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.model.Resource;

public interface IConfigurationService {

    public List<Folder> findFolders(FolderType type);
    
    public Flow findFlow(String id);

    public void deleteFolder(String folderId);

    public void delete(Agent agent);
    
    public void delete(AgentDeployment agentDeployment);
    
    public void delete(Flow flow, FlowStep flowStep);
    
    public void delete(FlowStepLink link);
    
    public void delete(Resource resource);    
    
    public boolean isDeployed(Flow flow);
    
    public List<Flow> findFlows();
           
    public Resource findResource(String id);
    
    public List<Resource> findResourcesByTypes(String projectVersionId, String ... types);
        
    public List<Agent> findAgents();
    
    public List<Agent> findAgentsInFolder(Folder folder);
    
    public List<Agent> findAgentsForHost(String hostName);
    
    public List<AgentDeployment> findAgentDeploymentsFor(Flow flow);
    
    public void deleteFlow(Flow flow);

    public void refresh(Flow flow);
    
    public void refresh(ProjectVersion projectVersion);
    
    public void refresh(Agent agent);
    
    public void refresh(Resource resource);
    
    public void refresh(Component component);
    
    public void save(AgentDeployment agentDeployment);
    
    public void save(Resource resource);
    
    public void save(FlowStep flowStep);    

    public void save(AbstractObject obj);

    public void save(Flow flow);
    
    public void save(Project project);
    
    public void save(ProjectVersion projectVersion);
    
    public List<AgentSummary> findUndeployedAgentsFor(String flowId);
    
    public void delete(AbstractObject obj);
    
    public void delete(Model model);
    
    public void delete(ModelEntity modelEntity);
    
    public void delete(ModelAttribute modelAttribute);
    
    public void delete(ModelEntityRelationship modelEntityRelationship);
    
    public void delete(ModelAttributeRelationship modelAttributeRelationship);
    
    public void refresh(Model model);
    
    public void refresh(ModelEntity modelEntity);
    
    public void refresh(ModelAttribute modelAttribute);
    
    public void refresh(ModelEntityRelationship modelEntityRelationship);
    
    public void refresh(ModelAttributeRelationship modelAttributeRelationship);
    
    public void save(Model model);
    
    public void save(ModelEntity modelEntity);
    
    public void save(ModelEntityRelationship modelEntityRelationship);

    public List<Project> findProjects();
    
    public List<Flow> findFlowsInProject(String projectVersionId);
    
    public List<Model> findModelsInProject(String projectVersionId);

    public List<Resource> findResourcesInProject(String projectVersionId);

}
