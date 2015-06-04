package org.jumpmind.symmetric.is.core.persist;

import java.util.List;
import java.util.Map;

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
import org.jumpmind.symmetric.is.core.model.GlobalSetting;
import org.jumpmind.symmetric.is.core.model.Group;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.ModelName;
import org.jumpmind.symmetric.is.core.model.Notification;
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

    public List<Folder> findFolders(String projectVersionId, FolderType type);
    
    public Folder findFirstFolderWithName(String name, FolderType type);
    
    public Flow findFlow(String id);
    
    public Model findModel(String id);
    
    public User findUser(String id);
    
    public User findUserByLoginId(String loginId);
    
    public List<User> findUsersByGroup(String groupId);
    
    public List<User> findUsers();
    
    public Group findGroup(String id);

    public List<Group> findGroups();

    public void delete(Folder folder);

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
    
    public Agent findAgent(String agentId);
    
    public List<Agent> findAgentsInFolder(Folder folder);
    
    public List<Agent> findAgentsForHost(String hostName);
    
    public List<AgentDeployment> findAgentDeployments();
    
    public List<AgentDeployment> findAgentDeploymentsFor(Flow flow);
    
    public AgentDeployment findAgentDeployment(String id);
    
    public List<AgentDeploymentSummary> findAgentDeploymentSummary(String agentId);

    public AgentResource findAgentResource(String agentId, String resourceId);
    
    public void deleteFlow(Flow flow);

    public void refresh(Flow flow);
    
    public void refresh(AgentDeployment deployment);
    
    public void refresh(ProjectVersion projectVersion);
    
    public void refresh(Agent agent);
    
    public void refresh(Resource resource);
    
    public void refresh(Component component);
    
    public void refresh(User user);
    
    public void refresh(Group group);

    public void save(AgentDeployment agentDeployment);
    
    public void save(Resource resource);
    
    public void save(FlowStep flowStep);    

    public void save(AbstractObject obj);
    
    public void save(Component component);

    public void save(Flow flow);
    
    public void save(Project project);
    
    public void save(ProjectVersion projectVersion);
    
    public void delete(AbstractObject obj);
    
    public void delete(Model model);
    
    public void delete(ModelEntity modelEntity);
    
    public void delete(ModelAttribute modelAttribute);
    
    public void delete(User user);
    
    public void delete(Group group);

    public void refresh(Model model);
    
    public void refresh(ModelEntity modelEntity);
    
    public void refresh(ModelAttribute modelAttribute);
    
    public void save(Model model);
    
    public void save(ModelEntity modelEntity);
    
    public ProjectVersion findProjectVersion(String projectVersionId);

    public List<Project> findProjects();
    
    public String export(ProjectVersion projectVersion);
    
    public boolean isUserLoginEnabled();
    
    public List<Notification> findNotifications();

    public List<Notification> findNotificationsForAgent(String agentId);

    public void refresh(Notification notification);

    public List<GlobalSetting> findGlobalSettings();

    public Map<String, String> findGlobalSettingsAsMap();
    
    public GlobalSetting findGlobalSetting(String name);
    
}
