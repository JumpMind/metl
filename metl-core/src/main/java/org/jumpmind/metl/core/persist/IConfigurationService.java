/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.persist;

import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.AgentName;
import org.jumpmind.metl.core.model.AgentResource;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentName;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.User;

public interface IConfigurationService {
    
    public boolean isInstalled();
        
    public List<FolderName> findFoldersInProject(String projectVersionId);
    
    public List<ComponentName> findComponentsInProject(String projectVersionId);
    
    public List<ModelName> findModelsInProject(String projectVersionId);
    
    public List<ResourceName> findResourcesInProject(String projectVersionId);
    
    public List<ComponentName> findSharedComponentsInProject(String projectVersionId);
    
    public List<FlowName> findFlowsInProject(String projectVersionId, boolean testFlows);

    public List<Folder> findFolders(String projectVersionId, FolderType type);
    
    public Folder findFirstFolderWithName(String name, FolderType type);
    
    public Flow findFlow(String id);
    
    public String getLastKnownVersion();
    
    public Model findModel(String id);
    
    public boolean isModelUsed(String id);
    
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
    
    public List<AgentName> findAgentsInFolder(Folder folder);
    
    public List<Agent> findAgentsForHost(String hostName);
    
    public List<AgentDeployment> findAgentDeploymentsFor(Flow flow);
    
    public AgentDeployment findAgentDeployment(String id);
    
    public List<AgentDeploymentSummary> findAgentDeploymentSummary(String agentId);

    public AgentResource findAgentResource(String agentId, String resourceId);
    
    public void deleteFlow(Flow flow);

    public void refresh(Flow flow);
    
    public void refresh(AgentDeployment deployment);
    
    public void refresh(ProjectVersion projectVersion);
    
    public void refresh(Agent agent);
    
    public void refreshAgentParameters(Agent agent);
    
    public void refresh(Resource resource);
    
    public void refresh(Component component, boolean readRelations);
    
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
    
    public void save(Model model);
    
    public void save(ModelEntity modelEntity);
    
    public ProjectVersion findProjectVersion(String projectVersionId);

    public List<Project> findProjects();
    
    public String export(ProjectVersion projectVersion);
    
    public String export(ProjectVersion projectVersion, Flow flow);
    
    public String export(Agent agent);
    
    public boolean isUserLoginEnabled();
    
    public List<Notification> findNotifications();

    public List<Notification> findNotificationsForAgent(String agentId);

    public List<Notification> findNotificationsForDeployment(AgentDeployment deployment);
    
    public void refresh(Notification notification);

    public List<GlobalSetting> findGlobalSettings();

    public Map<String, String> findGlobalSettingsAsMap();
    
    public GlobalSetting findGlobalSetting(String name);
    
    public List<Component> findDependentSharedComponents(String flowId);
    
    public List<Model> findDependentModels(String flowId);
    
    public List<Resource> findDependentResources(String flowId);
    
    public List<Flow> findAffectedFlowsByModel(String modelId);
    
    public List<Flow> findAffectedFlowsByResource(String resourceId);
    
    public List<Flow> findAffectedFlowsByFlow(String flowId);

    public FlowStep copy(FlowStep original);

    public Flow copy(Flow original);

    public Model copy(Model original);
    
}
