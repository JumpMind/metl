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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectLastUpdateTimeDescSorter;
import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentDeploymentParameter;
import org.jumpmind.metl.core.model.AgentName;
import org.jumpmind.metl.core.model.AgentParameter;
import org.jumpmind.metl.core.model.AgentResource;
import org.jumpmind.metl.core.model.AgentResourceSetting;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.ComponentName;
import org.jumpmind.metl.core.model.ComponentSetting;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.GroupPrivilege;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.ResourceSetting;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.model.UserGroup;
import org.jumpmind.metl.core.model.UserSetting;
import org.jumpmind.metl.core.util.NameValue;
import org.jumpmind.persist.IPersistenceManager;

abstract class AbstractConfigurationService extends AbstractService implements
        IConfigurationService {

    AbstractConfigurationService(IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
    }
    
    @Override
    public List<FlowName> findFlowsInProject(String projectVersionId, boolean test) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        params.put("deleted", 0);
        params.put("test", test ? 1 : 0);
        return find(FlowName.class, params, Flow.class);
    }   

    @Override
    public List<ModelName> findModelsInProject(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        params.put("deleted", 0);
        return find(ModelName.class, params, Model.class);
    }
    
    
    @Override
    public boolean isModelUsed(String id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("outputModelId", id);
        params.put("deleted", false);
        if (count(Component.class, params) == 0) {
            params.remove("outputModelId");
            params.put("inputModelId", id);
            if (count(Component.class, params) == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<ResourceName> findResourcesInProject(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        params.put("deleted", 0);
        return find(ResourceName.class, params, Resource.class);
    }

    @Override
    public List<ComponentName> findSharedComponentsInProject(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        params.put("deleted", 0);
        params.put("shared", 1);
        return find(ComponentName.class, params, Component.class);
    }

    @Override
    public List<ComponentName> findComponentsInProject(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        params.put("deleted", 0);
        return find(ComponentName.class, params, Component.class);
    }

    @Override
    public List<FolderName> findFoldersInProject(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        params.put("deleted", 0);
        return find(FolderName.class, params, Folder.class);
    }

    @Override
    public Flow findFlow(String id) {
        Flow flowVersion = new Flow();
        flowVersion.setId(id);
        refresh(flowVersion);
        return flowVersion;
    }
    
    @Override
    public Folder findFirstFolderWithName(String name, FolderType type) {
        Map<String, Object> byType = new HashMap<String, Object>();
        byType.put("type", type.name());
        if (isNotBlank(name)) {
            byType.put("name", name);
        }
        byType.put("deleted", 0);
        return findOne(Folder.class, byType);
    }

    @Override
    public List<Folder> findFolders(String projectVersionId, FolderType type) {
        ArrayList<Folder> allFolders = new ArrayList<Folder>(foldersById(projectVersionId, type)
                .values());
        List<Folder> rootFolders = new ArrayList<Folder>();
        Collections.sort(allFolders, new Comparator<Folder>() {
            @Override
            public int compare(Folder o1, Folder o2) {
                return o1.getCreateTime().compareTo(o2.getCreateTime());
            }
        });
        for (Folder folder : allFolders) {
            boolean foundAParent = false;
            for (Folder parentFolder : allFolders) {
                if (parentFolder.isParentOf(folder)) {
                    parentFolder.getChildren().add(folder);
                    folder.setParent(parentFolder);
                    foundAParent = true;
                }
            }
            if (!foundAParent) {
                rootFolders.add(folder);
            }
        }

        return rootFolders;
    }

    protected Map<String, Folder> foldersById(String projectVersionId, FolderType type) {
        Map<String, Object> byType = new HashMap<String, Object>();
        byType.put("type", type.name());
        if (isNotBlank(projectVersionId)) {
            byType.put("projectVersionId", projectVersionId);
        }
        byType.put("deleted", 0);
        List<Folder> folders = find(Folder.class, byType);

        Map<String, Folder> all = new HashMap<String, Folder>();
        for (Folder folder : folders) {
            all.put(folder.getId(), folder);
        }
        return all;
    }

    @Override
    public List<FlowName> findFlows() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deleted", 0);
        List<FlowName> flows = find(FlowName.class, params);
        AbstractObjectNameBasedSorter.sort(flows);
        return flows;
    }

    @Override
    public List<Resource> findResourcesByTypes(String projectVersionId, String... types) {
        List<Resource> list = new ArrayList<Resource>();
        if (types != null) {
            for (String type : types) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("type", type);
                params.put("deleted", 0);
                params.put("projectVersionId", projectVersionId);
                List<Resource> datas = find(Resource.class, params);
                list.addAll(buildResource(datas));
            }
        }
        return list;
    }

    @Override
    public ProjectVersion findProjectVersion(String projectVersionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", projectVersionId);
        ProjectVersion projectVersion = 
        findOne(ProjectVersion.class, params);
        if (projectVersion != null) {
            refresh(projectVersion);
        }
        return projectVersion;
    }

    @Override
    public List<Project> findProjects() {
        List<Project> list = persistenceManager.find(Project.class, new NameValue("deleted", 0),
                null, null, tableName(Project.class));

        List<ProjectVersion> versions = persistenceManager.find(ProjectVersion.class,
                new NameValue("deleted", 0), null, null, tableName(ProjectVersion.class));
        for (ProjectVersion projectVersion : versions) {
            for (Project project : list) {
                if (project.getId().equals(projectVersion.getProjectId())) {
                    projectVersion.setProject(project);
                    project.getProjectVersions().add(projectVersion);
                    break;
                }
            }
        }
        return list;
    }

    @Override
    public List<AgentName> findAgentsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        String folderId = null;
        if (folder != null) {
            folderId = folder.getId();
        }
        params.put("folderId", folderId);
        params.put("deleted", 0);
        return find(AgentName.class, params, Agent.class);
    }

    @Override
    public List<Agent> findAgentsForHost(String hostName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("host", hostName);
        params.put("deleted", 0);
        return findAgents(params);
    }

    @Override
    public List<Agent> findAgents() {
        return persistenceManager.find(Agent.class, null, null, tableName(Agent.class));
    }    

    @Override
    public Agent findAgent(String agentId) {
        Agent agent = findOne(Agent.class, new NameValue("id", agentId));
        refreshAgentParameters(agent);
        refreshAgentResourceSettings(agent);
        return agent;
    }

    protected List<Agent> findAgents(Map<String, Object> params) {
        return findAgents(params, null);
    }

    protected List<Agent> findAgents(Map<String, Object> params, Folder folder) {
        List<Agent> list = persistenceManager.find(Agent.class, params, null, null,
                tableName(Agent.class));
        Map<String, Folder> folderMapById = new HashMap<String, Folder>();
        if (folder != null) {
            folderMapById.put(folder.getId(), folder);
        } else {
            folderMapById = foldersById(null, FolderType.AGENT);
        }

        for (Agent agent : list) {
            refreshAgentParameters(agent);
            refreshAgentResourceSettings(agent);
            refreshAgentDeployments(agent);
            agent.setFolder(folderMapById.get(agent.getFolderId()));
        }

        Collections.sort(list, new Comparator<Agent>() {
            @Override
            public int compare(Agent o1, Agent o2) {
                return o1.getCreateTime().compareTo(o2.getCreateTime());
            }
        });
        return list;
    }

    public synchronized void refreshAgentParameters(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        List<AgentParameter> parameters = persistenceManager.find(AgentParameter.class, settingParams,
                null, null, tableName(AgentParameter.class));
        agent.setAgentParameters(parameters);
    }

    protected void refreshAgentResourceSettings(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        List<AgentResourceSetting> settings = persistenceManager.find(AgentResourceSetting.class,
                settingParams, null, null, tableName(AgentResourceSetting.class));
        AbstractObjectLastUpdateTimeDescSorter.sort(settings);
        agent.setAgentResourceSettings(settings);
    }

    protected void refreshAgentDeployments(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        List<AgentDeployment> deployments = persistenceManager.find(AgentDeployment.class,
                settingParams, null, null, tableName(AgentDeployment.class));
        agent.setAgentDeployments(deployments);
        for (AgentDeployment agentDeployment : deployments) {
            refreshAgentDeploymentRelations(agentDeployment);
            refresh(agentDeployment.getFlow());
        }
    }

    protected void refreshAgentDeploymentRelations(AgentDeployment agentDeployment) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("agentDeploymentId", agentDeployment.getId());
        agentDeployment.setAgentDeploymentParameters(persistenceManager.find(
                AgentDeploymentParameter.class, params, null, null,
                tableName(AgentDeploymentParameter.class)));
    }

    @Override
    public AgentDeployment findAgentDeployment(String id) {
        AgentDeployment agentDeployment = findOne(AgentDeployment.class, new NameValue("id", id));
        if (agentDeployment != null) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("agentDeploymentId", agentDeployment.getId());
            List<AgentDeploymentParameter> deploymentParams = persistenceManager.find(
                    AgentDeploymentParameter.class, params, null, null,
                    tableName(AgentDeploymentParameter.class));
            agentDeployment.setAgentDeploymentParameters(deploymentParams);
        }
        return agentDeployment;
    }

    @Override
    public List<AgentDeployment> findAgentDeploymentsFor(Flow flow) {
        List<AgentDeployment> deployments = persistenceManager
                .find(AgentDeployment.class, new NameValue("flowId", flow.getId()), null, null,
                        tableName(AgentDeployment.class));
        for (AgentDeployment deployment : deployments) {
            deployment.setFlow(flow);
        }
        return deployments;
    }

    @Override
    public AgentResource findAgentResource(String agentId, String resourceId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("agentId", agentId);
        params.put("resourceId", resourceId);
        List<AgentResourceSetting> settings = persistenceManager.find(AgentResourceSetting.class,
                params, null, null, tableName(AgentResourceSetting.class));

        Resource resource = findResource(resourceId);
        for (Setting resourceSetting : resource.getSettings()) {
            boolean exists = false;
            for (AgentResourceSetting setting : settings) {
                if (setting.getName().equals(resourceSetting.getName())) {
                    exists = true;
                }
            }
            if (!exists) {
                AgentResourceSetting setting = new AgentResourceSetting();
                setting.setId(resourceId);
                setting.setAgentId(agentId);
                setting.setResourceId(resourceId);
                setting.setName(resourceSetting.getName());
                setting.setValue(resourceSetting.getValue());
                settings.add(setting);
            }
        }

        AgentResource agentResource = new AgentResource();
        agentResource.setId(resource.getId());
        agentResource.setAgentId(agentId);
        agentResource.setType(resource.getType());
        agentResource.setSettings(settings);
        return agentResource;
    }

    @Override
    public Resource findResource(String id) {
        Resource resource = findOne(Resource.class, new NameValue("id", id));
        if (resource != null) {
            refresh(resource);
        }
        return resource;
    }

    protected Component findComponent(String id, boolean readRelations) {
        Component component = new Component();
        component.setId(id);
        refresh(component, readRelations);
        return component;
    }

    @Override
    public void refresh(ProjectVersion projectVersion) {
        refresh((AbstractObject) projectVersion);
        refresh((AbstractObject) projectVersion.getProject());
    }

    @Override
    public void refresh(Component component, boolean readRelations) {

        persistenceManager.refresh(component, null, null, tableName(Component.class));

        if (readRelations) {
            if (isNotBlank(component.getInputModelId())) {
                component.setInputModel(findModel(component.getInputModelId()));
            }
            if (isNotBlank(component.getOutputModelId())) {
                component.setOutputModel(findModel(component.getOutputModelId()));
            }
        }

        List<ComponentSetting> settings = find(ComponentSetting.class, new NameValue("componentId",
                component.getId()));
        AbstractObjectLastUpdateTimeDescSorter.sort(settings);
        component.setSettings(settings);

        List<ComponentEntitySetting> entitySettings = find(ComponentEntitySetting.class,
                new NameValue("componentId", component.getId()));
        AbstractObjectLastUpdateTimeDescSorter.sort(entitySettings);
        component.setEntitySettings(entitySettings);

        List<ComponentAttributeSetting> attributeSettings = find(ComponentAttributeSetting.class,
                new NameValue("componentId", component.getId()));
        AbstractObjectLastUpdateTimeDescSorter.sort(attributeSettings);
        component.setAttributeSettings(attributeSettings);

        if (readRelations) {
            component.setResource(findResource(component.getResourceId()));
        }

    }

    @Override
    public Model findModel(String id) {
        Model model = new Model(id);
        refresh(model);
        return model;
    }
    
    abstract protected List<ModelAttribute> findAllAttributesForModel(String modelId);

    protected Model refreshModelRelations(Model model) {
        model.getModelEntities().clear();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("modelId", model.getId());
        List<ModelEntity> entities = persistenceManager.find(ModelEntity.class, params,
                null, null, tableName(ModelEntity.class));
        List<ModelAttribute> attributes = findAllAttributesForModel(model.getId());
        Map<String, ModelEntity> byModelEntityId = new HashMap<String, ModelEntity>();
        for (ModelEntity entity : entities) {
            byModelEntityId.put(entity.getId(), entity);
            model.getModelEntities().add(entity);
        }
        
        for (ModelAttribute modelAttribute : attributes) {
            byModelEntityId.get(modelAttribute.getEntityId()).getModelAttributes().add(modelAttribute);            
        }

        AbstractObjectNameBasedSorter.sort(entities);
        return model;
    }

    protected List<Resource> buildResource(List<Resource> datas) {
        return buildResource(datas.toArray(new Resource[datas.size()]));
    }

    protected List<Resource> buildResource(Resource... resources) {
        List<Resource> list = new ArrayList<Resource>();
        for (Resource resource : resources) {
            Map<String, Object> settingParams = new HashMap<String, Object>();
            settingParams.put("resourceId", resource.getId());
            List<ResourceSetting> settings = find(ResourceSetting.class, settingParams);
            resource.setSettings(settings);
            list.add(resource);
        }
        return list;
    }

    @Override
    public User findUser(String id) {
        User user = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        List<User> users = persistenceManager.find(User.class, params, null, null,
                tableName(User.class));
        if (users.size() > 0) {
            user = users.get(0);
            refresh(user);
        }
        return user;
    }

    @Override
    public User findUserByLoginId(String loginId) {
        User user = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("loginId", loginId);
        List<User> users = persistenceManager.find(User.class, params, null, null,
                tableName(User.class));
        if (users.size() > 0) {
            user = users.get(0);
            refresh(user);
        }
        return user;
    }

    @Override
    public List<User> findUsersByGroup(String groupId) {
        List<User> users = new ArrayList<User>();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", groupId);
        List<UserGroup> userGroups = persistenceManager.find(UserGroup.class, params, null, null,
                tableName(UserGroup.class));
        for (UserGroup userGroup : userGroups) {
            users.add(findUser(userGroup.getUserId()));
        }       
        return users;
    }

    @Override
    public List<User> findUsers() {
        return persistenceManager.find(User.class, null, null, null, tableName(User.class));
    }

    @Override
    public Group findGroup(String id) {
        Group group = null;
        Map<String, Object> params = new HashMap<String, Object>();
        params = new HashMap<String, Object>();
        params.put("id", id);
        List<Group> groups = persistenceManager.find(Group.class, params, null, null,
                tableName(Group.class));
        if (groups.size() > 0) {
            group = groups.get(0);
            refresh(group);
        }
        return group;
    }

    @Override
    public List<Group> findGroups() {
        return persistenceManager.find(Group.class, null, null, null, tableName(Group.class));
    }

    @Override
    public void delete(AgentDeployment agentDeployment) {
        List<AgentDeploymentParameter> params = agentDeployment.getAgentDeploymentParameters();
        for (AgentDeploymentParameter agentDeploymentParameter : params) {
            delete((AbstractObject) agentDeploymentParameter);
        }
        delete((AbstractObject) agentDeployment);
    }

    @Override
    public void delete(Flow flow, FlowStep flowStep) {
        List<FlowStepLink> links = flow.removeFlowStepLinks(flowStep.getId());
        for (FlowStepLink link : links) {
            delete(link);
        }

        flow.removeFlowStep(flowStep);
        delete(flowStep);

        Component comp = flowStep.getComponent();
        if (!comp.isShared()) {
            delete(comp);
        }
    }

    public void delete(Component comp) {
        comp.setDeleted(true);
        save((AbstractObject) comp);
    }

    @Override
    public void delete(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("parentFolderId", folder.getId());
        List<Folder> folders = find(Folder.class, params);
        for (Folder child : folders) {
            delete(child);
        }

        params = new HashMap<String, Object>();
        params.put("folderId", folder.getId());

        List<Component> comps = find(Component.class, params);
        for (Component component : comps) {
            delete(component);
        }

        List<Model> models = find(Model.class, params);
        for (Model model : models) {
            delete(model);
        }

        List<Flow> flows = find(Flow.class, params);
        for (Flow flow : flows) {
            delete(flow);
        }

        List<Resource> resources = find(Resource.class, params);
        for (Resource resource : resources) {
            delete(resource);
        }

        List<Agent> agents = find(Agent.class, params);
        for (Agent agent : agents) {
            delete(agent);
        }

        folder.setDeleted(true);
        save((AbstractObject) folder);
    }

    @Override
    public void delete(Resource resource) {
        resource.setDeleted(true);
        save((AbstractObject) resource);
    }

    @Override
    public void delete(Agent agent) {
        agent.setDeleted(true);
        save((AbstractObject) agent);
    }

    @Override
    public boolean delete(FlowStepLink link) {
        return persistenceManager.delete(link, null, null, tableName(FlowStepLink.class));
    }

    @Override
    public void deleteFlow(Flow flow) {
        flow.setDeleted(true);
        List<FlowStep> steps = flow.getFlowSteps();
        for (FlowStep flowStep : steps) {
            if (!flowStep.getComponent().isShared()) {
                flowStep.getComponent().setDeleted(true);
                save(flowStep.getComponent());
            }
        }
        save((AbstractObject) flow);
    }

    @Override
    public void delete(User user) {
        refresh(user);
        for (Setting setting : user.getSettings()) {
            persistenceManager.delete(setting, null, null, tableName(UserSetting.class));
        }
        for (Group group : user.getGroups()) {
            persistenceManager.delete(new UserGroup(user.getId(), group.getId()), null, null, tableName(UserGroup.class));
        }

        persistenceManager.delete(user, null, null, tableName(User.class));
    }

    @Override
    public void delete(Group group) {
        refresh(group);
        for (GroupPrivilege groupPriv : group.getGroupPrivileges()) {
            persistenceManager.delete(groupPriv, null, null, tableName(GroupPrivilege.class));
        }
        persistenceManager.delete(group, null, null, tableName(Group.class));
    }

    @Override
    public void refresh(Resource resource) {
        refresh((AbstractObject) resource);

        Map<String, Object> folderParams = new HashMap<String, Object>();
        folderParams.put("id", resource.getFolderId());
        resource.setFolder(findOne(Folder.class, folderParams));

        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("resourceId", resource.getId());
        List<? extends Setting> settings = find(ResourceSetting.class, settingParams);
        resource.setSettings(settings);
    }

    @Override
    public void refresh(Agent agent) {
        refresh((AbstractObject) agent);
        if (agent.getFolder() != null) {
            refresh(agent.getFolder());
        }
        refreshAgentParameters(agent);
        refreshAgentResourceSettings(agent);
        refreshAgentDeployments(agent);
    }

    @Override
    public void refresh(Flow flow) {
        refresh((AbstractObject) flow);
        refreshFlowRelations(flow);
    }

    @Override
    public void refresh(AgentDeployment deployment) {
        refresh((AbstractObject) deployment);
        refreshAgentDeploymentRelations(deployment);
    }
    
    @Override
    public void refresh(User user) {
        Map<String, Object> params = new HashMap<String, Object>();
        params = new HashMap<String, Object>();
        params.put("userId", user.getId());
        user.setSettings(persistenceManager.find(UserSetting.class, params, null, null,
                tableName(UserSetting.class)));

        List<Group> groups = new ArrayList<Group>();
        List<UserGroup> userGroups = persistenceManager.find(UserGroup.class, params, null, null,
                tableName(UserGroup.class));
        for (UserGroup userGroup : userGroups) {
            groups.add(findGroup(userGroup.getGroupId()));
        }
        user.setGroups(groups);
    }

    @Override
    public void refresh(Group group) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", group.getId());
        group.setGroupPrivileges(persistenceManager.find(GroupPrivilege.class, params, null, null,
                tableName(GroupPrivilege.class)));
    }

    private void refreshFlowRelations(Flow flow) {
        flow.getFlowSteps().clear();
        flow.getFlowStepLinks().clear();
        Map<String, Object> versionParams = new HashMap<String, Object>();
        versionParams.put("flowId", flow.getId());

        flow.setFlowParameters(persistenceManager.find(FlowParameter.class, versionParams, null,
                null, tableName(FlowParameter.class)));

        List<FlowStep> steps = persistenceManager.find(FlowStep.class, versionParams, null, null,
                tableName(FlowStep.class));

        Collections.sort(steps, new Comparator<FlowStep>() {
            @Override
            public int compare(FlowStep o1, FlowStep o2) {
                return new Integer(o1.getX()).compareTo(new Integer(o2.getX()));
            }
        });
        
        Map<String, Model> models = new HashMap<>();
        Map<String, Resource> resources = new HashMap<>();
        
        for (FlowStep step : steps) {
            Component component = findComponent(step.getComponentId(), false);            
            step.setComponent(component);
            flow.getFlowSteps().add(step);            
            
            String modelId = component.getOutputModelId();
            if (isNotBlank(modelId)) {
                Model model = models.get(modelId);
                if (model == null) {
                    model = findModel(modelId);
                    models.put(modelId, model);
                }
                component.setOutputModel(model);
            }
            
            modelId = component.getInputModelId();
            if (isNotBlank(modelId)) {
                Model model = models.get(modelId);
                if (model == null) {
                    model = findModel(modelId);
                    models.put(modelId, model);
                }
                component.setInputModel(model);
            }

            String resourceId = component.getResourceId(); 
            if (isNotBlank(resourceId)) {
                Resource resource = resources.get(resourceId);
                if (resource == null) {
                    resource = findResource(resourceId);
                    resources.put(resourceId, resource);
                }
                component.setResource(resource);
            }


            Map<String, Object> linkParams = new HashMap<String, Object>();
            linkParams.put("sourceStepId", step.getId());

            List<FlowStepLink> dataLinks = persistenceManager.find(FlowStepLink.class, linkParams,
                    null, null, tableName(FlowStepLink.class));
            for (FlowStepLink dataLink : dataLinks) {
                flow.getFlowStepLinks().add(dataLink);
            }
        }
    }

    @Override
    public void save(AgentDeployment agentDeployment) {
        save((AbstractObject) agentDeployment);
        List<AgentDeploymentParameter> parameters = agentDeployment.getAgentDeploymentParameters();
        for (AgentDeploymentParameter agentDeploymentParameter : parameters) {
            save((AbstractObject) agentDeploymentParameter);
        }
    }

    @Override
    public void save(Component component) {
        save((AbstractObject) component);

        List<ComponentAttributeSetting> aSettings = component.getAttributeSettings();
        if (aSettings != null) {
            for (ComponentAttributeSetting componentAttributeSetting : aSettings) {
                save(componentAttributeSetting);
            }
        }
        
        List<ComponentEntitySetting> eSettings = component.getEntitySettings();
        if (eSettings != null) {
            for (ComponentEntitySetting componentEntitySetting : eSettings) {
                save(componentEntitySetting);
            }
        }
        
        List<Setting> settings = component.getSettings();
        if (settings != null) {
            for (Setting setting : settings) {
                save(setting);
            }
        }
    }
    
    @Override
    public void save(Project project) {
        save((AbstractObject) project);
    }

    @Override
    public void save(ProjectVersion projectVersion) {
        save((AbstractObject) projectVersion);
    }

    @Override
    public void save(Resource resource) {
        save((AbstractObject) resource);
        List<Setting> settings = resource.getSettings();
        for (Setting settingData : settings) {
            save(settingData);
        }
    }

    @Override
    public void save(FlowStep flowStep) {
        Component component = flowStep.getComponent();
        if (!component.isShared()) {
            save(component);
        }
        save((AbstractObject) flowStep);
    }

    @Override
    public void save(Flow flow) {
        save((AbstractObject) flow);

        List<FlowStep> flowSteps = flow.getFlowSteps();
        for (FlowStep flowStep : flowSteps) {
            save(flowStep);
        }

        List<FlowStepLink> links = flow.getFlowStepLinks();
        for (FlowStepLink link : links) {
            save(link);
        }

        List<FlowParameter> parameters = flow.getFlowParameters();
        for (FlowParameter parm : parameters) {
            save(parm);
        }

    }

    @Override
    public void delete(Model model) {
        model.setDeleted(true);
        save((AbstractObject) model);
    }

    @Override
    public void delete(ModelEntity modelEntity) {
        List<ComponentEntitySetting> settings = persistenceManager.find(
                ComponentEntitySetting.class, new NameValue("entityId", modelEntity.getId()), null,
                null, tableName(ComponentEntitySetting.class));
        for (ComponentEntitySetting setting : settings) {
            delete(setting);
        }

        for (ModelAttribute modelAttribute : modelEntity.getModelAttributes()) {
            delete(modelAttribute);
        }

        persistenceManager.delete(modelEntity, null, null, tableName(ModelEntity.class));
    }

    @Override
    public void delete(ModelAttribute modelAttribute) {
        List<ComponentAttributeSetting> attributeSettings = persistenceManager.find(
                ComponentAttributeSetting.class,
                new NameValue("attributeId", modelAttribute.getId()), null, null,
                tableName(ComponentAttributeSetting.class));
        for (ComponentAttributeSetting setting : attributeSettings) {
            delete(setting);
        }
        persistenceManager.delete(modelAttribute, null, null, tableName(ModelAttribute.class));
    }

    @Override
    public void refresh(Model model) {
        refresh((AbstractObject) model);

        Map<String, Object> folderParams = new HashMap<String, Object>();
        folderParams.put("id", model.getFolderId());
        model.setFolder(findOne(Folder.class, folderParams));

        refreshModelRelations(model);
    }

    @Override
    public void save(Model model) {
        save((AbstractObject) model);
        for (ModelEntity modelEntity : model.getModelEntities()) {
            save(modelEntity);
        }
    }

    @Override
    public void save(ModelEntity modelEntity) {
        save((AbstractObject) modelEntity);
        for (ModelAttribute modelAttribute : modelEntity.getModelAttributes()) {
            save(modelAttribute);
        }
    }
    
    @Override
    public List<Notification> findNotifications() {
        return persistenceManager.find(Notification.class, null, null, null, tableName(Notification.class));
    }

    @Override
    public List<Notification> findNotificationsForAgent(String agentId) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("level", Notification.Level.AGENT.toString());
        param.put("linkId", agentId);
        param.put("enabled", 1);
        List<Notification> agentNotifications = persistenceManager.find(Notification.class, param, null, null, tableName(Notification.class));

        param = new HashMap<String, Object>();
        param.put("level", Notification.Level.GLOBAL.toString());
        param.put("enabled", 1);
        List<Notification> notifications = persistenceManager.find(Notification.class, param, null, null, tableName(Notification.class));
        notifications.addAll(agentNotifications);
        return notifications;
    }

    @Override
    public List<Notification> findNotificationsForDeployment(AgentDeployment deployment) {
        List<Notification> notifications = findNotificationsForAgent(deployment.getAgentId());
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("level", Notification.Level.DEPLOYMENT.toString());
        param.put("linkId", deployment.getId());
        param.put("enabled", 1);
        List<Notification> agentNotifications = persistenceManager.find(Notification.class, param, null, null, tableName(Notification.class));
        notifications.addAll(agentNotifications);
        return notifications;
    }
    
    @Override
    public void refresh(Notification notification) {
        refresh((AbstractObject) notification);        
    }

    @Override
    public List<GlobalSetting> findGlobalSettings() {
        return persistenceManager.find(GlobalSetting.class, null, null, tableName(GlobalSetting.class));
    }

    @Override
    public GlobalSetting findGlobalSetting(String name) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("name", name);
        List<GlobalSetting> settings =  persistenceManager.find(GlobalSetting.class, param, null, null, tableName(GlobalSetting.class));
        if (settings.size() > 0) {
            return settings.get(0);
        }
        return null;
    }

    @Override
    public Map<String, String> findGlobalSettingsAsMap() {
        Map<String, String> globalSettings = new HashMap<String, String>();
        for (GlobalSetting setting : findGlobalSettings()) {
            globalSettings.put(setting.getName(), setting.getValue());
        }
        return globalSettings;
    }

}
