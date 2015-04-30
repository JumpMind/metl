package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentSummary;
import org.jumpmind.symmetric.is.core.model.AgentResource;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentName;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowName;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderName;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Group;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelAttributeRelationship;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.ModelEntityRelationship;
import org.jumpmind.symmetric.is.core.model.ModelName;
import org.jumpmind.symmetric.is.core.model.Project;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.ResourceName;
import org.jumpmind.symmetric.is.core.model.User;

public interface IConfigurationService {
    
    public List<FolderName> findFoldersInProject(String projectVersionId);
    
    public List<ComponentName> findComponentsInProject(String projectVersionId);
    
    public List<ModelName> findModelsInProject(String projectVersionId);
    
    public List<ResourceName> findResourcesInProject(String projectVersionId);
    
    public List<ComponentName> findSharedComponentsInProject(String projectVersionId);
    
    public List<FlowName> findFlowsInProject(String projectVersionId);

    public List<Folder> findFolders(FolderType type);
    
    public Flow findFlow(String id);
    
    public Model findModel(String id);
    
    public User findUser(String id);
    
    public User findUserByLoginId(String loginId);
    
    public List<User> findUsers();
    
    public Group findGroup(String id);

    public void deleteFolder(String folderId);

    public void delete(Agent agent);
    
    public void delete(AgentDeployment agentDeployment);
    
    public void delete(Flow flow, FlowStep flowStep);
    
    public boolean delete(FlowStepLink link);
    
    public void delete(Resource resource);    
    
    public boolean isDeployed(Flow flow);
    
    public List<FlowName> findFlows();
           
    public Resource findResource(String id);
    
    public List<Resource> findResourcesByTypes(String projectVersionId, String ... types);
        
    public List<Agent> findAgents();
    
    public List<Agent> findAgentsInFolder(Folder folder);
    
    public List<Agent> findAgentsForHost(String hostName);
    
    public List<AgentDeployment> findAgentDeploymentsFor(Flow flow);
    
    public AgentDeployment findAgentDeployment(String id);
    
    public List<AgentDeploymentSummary> findAgentDeploymentSummary(String agentId);

    public AgentResource findAgentResource(String agentId, String resourceId);
    
    public void deleteFlow(Flow flow);

    public void refresh(Flow flow);
    
    public void refresh(ProjectVersion projectVersion);
    
    public void refresh(Agent agent);
    
    public void refresh(Resource resource);
    
    public void refresh(Component component);
    
    public void refresh(User user);

    public void save(AgentDeployment agentDeployment);
    
    public void save(Resource resource);
    
    public void save(FlowStep flowStep);    

    public void save(AbstractObject obj);

    public void save(Flow flow);
    
    public void save(Project project);
    
    public void save(ProjectVersion projectVersion);
    
    public void delete(AbstractObject obj);
    
    public void delete(Model model);
    
    public void delete(ModelEntity modelEntity);
    
    public void delete(ModelAttribute modelAttribute);
    
    public void delete(ModelEntityRelationship modelEntityRelationship);
    
    public void delete(ModelAttributeRelationship modelAttributeRelationship);

    public void delete(User user);

    public void refresh(Model model);
    
    public void refresh(ModelEntity modelEntity);
    
    public void refresh(ModelAttribute modelAttribute);
    
    public void refresh(ModelEntityRelationship modelEntityRelationship);
    
    public void refresh(ModelAttributeRelationship modelAttributeRelationship);
    
    public void save(Model model);
    
    public void save(ModelEntity modelEntity);
    
    public void save(ModelEntityRelationship modelEntityRelationship);

    public List<Project> findProjects();
    
    public String export(ProjectVersion projectVersion);
    
    public boolean isUserLoginEnabled();
}
