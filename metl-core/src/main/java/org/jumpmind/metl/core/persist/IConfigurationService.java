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
import org.jumpmind.metl.core.model.AuditEvent;
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
import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDefinitionPlugin;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.model.UserHist;
import org.jumpmind.properties.TypedProperties;

public interface IConfigurationService {

    public List<ProjectVersionDefinitionPlugin> findProjectVersionComponentPlugins(String projectVersionId);

    public void doInBackground();

    public FlowStep copy(FlowStep original);

    public List<PluginRepository> findPluginRepositories();

    public List<Plugin> findActivePlugins();

    public List<Plugin> findUnusedPlugins();

    public List<Plugin> findPlugins();

    public void delete(Agent agent);

    public void delete(AgentDeployment agentDeployment);

    public void delete(Flow flow, FlowStep flowStep);

    public boolean delete(FlowStepLink link);

    public void delete(Folder folder);

    public void delete(Resource resource);

    public void deleteFlow(Flow flow);

    public List<Flow> findAffectedFlowsByFlow(String flowId);

    public List<Flow> findAffectedFlowsByModel(String modelId);

    public List<Flow> findAffectedFlowsByResource(String resourceId);

    public Agent findAgent(String agentId, boolean includeDeployments);

    public AgentDeployment findAgentDeployment(String id);

    public List<AgentDeploymentSummary> findAgentDeploymentSummary(String agentId);

    public List<ProjectVersionDependency> findProjectDependencies(String projectVersionId);

    public AgentResource findAgentResource(String agentId, String resourceId);

    public List<Agent> findAgents();

    public List<AgentName> findAgentsInFolder(Folder folder);

    public void refresh(PluginRepository pluginRepository);

    public List<ComponentName> findComponentsInProject(String projectVersionId);

    public void save(Plugin plugin);

    public void save(ProjectVersionDefinitionPlugin projectVersionComponentPlugin);

    public void delete(AbstractObject obj);

    public void delete(Model model);

    public void delete(ModelEntity modelEntity);

    public void delete(ModelAttribute modelAttribute);

    public void delete(User user);

    public void delete(Group group);

    public List<String> findAllProjectVersionIds();

    public String export(Agent agent);

    public List<Flow> findDependentFlows(String projectVersionId);

    public List<Model> findDependentModels(String flowId);

    public List<Resource> findDependentResources(String flowId);

    public List<Component> findDependentSharedComponents(String flowId);

    public Folder findFirstFolderWithName(String name, FolderType type);

    public Flow findFlow(String id);

    public List<FlowName> findFlows();

    public List<FlowName> findFlowsInProject(String projectVersionId, boolean testFlows);

    public List<Folder> findFolders(String projectVersionId, FolderType type);

    public List<FolderName> findFoldersInProject(String projectVersionId);

    public GlobalSetting findGlobalSetting(String name);

    public List<GlobalSetting> findGlobalSettings();

    public TypedProperties findGlobalSetttingsAsProperties();

    public Map<String, String> findGlobalSettingsAsMap();

    public Group findGroup(String id);

    public List<Group> findGroups();

    public Model findModel(String id);

    public List<ModelName> findModelsInProject(String projectVersionId);

    public List<Notification> findNotifications();

    public List<Notification> findNotificationsForAgent(String agentId);

    public List<Notification> findNotificationsForDeployment(AgentDeployment deployment);

    public List<AuditEvent> findAuditEvents(int limit);

    public List<Project> findProjects();

    public ProjectVersion findProjectVersion(String projectVersionId);

    public Resource findResource(String id);    
    
    public List<Flow> findFlowsByName(String projectVersionId, String flowName);
    
    public List<Resource> findResourcesByName(String projectVersionId, String resourceName);

    public List<Model> findModelsByName(String projectVersionId, String modelName);

    public List<Resource> findResourcesByTypes(String projectVersionId, String... types);

    public List<ResourceName> findResourcesInProject(String projectVersionId);

    public Resource findPreviousVersionResource(Resource currentResource);

    public List<ComponentName> findSharedComponentsInProject(String projectVersionId);

    public User findUser(String id);

    public User findUserByLoginId(String loginId);

    public List<UserHist> findUserHist(String id);

    public List<User> findUsers();

    public List<User> findUsersByGroup(String groupId);

    public String getLastKnownVersion();

    public boolean isDeployed(Flow flow);

    public boolean isInstalled();

    public boolean isModelUsed(String id);

    public boolean isUserLoginEnabled();

    public void refresh(Component component, boolean readRelations);

    public void refresh(Group group);

    public void refresh(Model model);

    public void refresh(Notification notification);

    public void refresh(Project project);

    public void refresh(ProjectVersion projectVersion);

    public void refresh(Resource resource);

    public void refresh(User user);

    public void refreshAgentParameters(Agent agent);

    public void save(AbstractObject obj);

    public void save(AgentDeployment agentDeployment);

    public void save(Component component);

    public void save(Flow flow);

    public void save(FlowStep flowStep);

    public void save(Model model);

    public void save(ModelEntity modelEntity);

    public void save(Project project);

    public void save(ProjectVersion projectVersion);

    public void save(Resource resource);

    public void save(Setting setting);

    public ProjectVersion saveNewVersion(String newVersionLabel, ProjectVersion original);

    public void savePassword(User user, String newPassword);

    Group findGroupByName(String name);

    public Flow copy(Flow original);

    public Flow copy(Map<String, AbstractObject> oldToNewUUIDMapping, Flow original, boolean newProjectVersion);

    public Model copy(Model original);
    
    public Model copy(Map<String, AbstractObject> oldToNewUUIDMapping, Model original);
    
    public Resource copy(Resource original);
    
    public Resource copy(Map<String, AbstractObject> oldToNewUUIDMapping, Resource original);

    public void delete(Plugin plugin);

    Map<String, ProjectVersion> findProjectVersions();

}
