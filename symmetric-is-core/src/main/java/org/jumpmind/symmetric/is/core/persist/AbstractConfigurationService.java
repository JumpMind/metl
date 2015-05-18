package org.jumpmind.symmetric.is.core.persist;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentParameter;
import org.jumpmind.symmetric.is.core.model.AgentResource;
import org.jumpmind.symmetric.is.core.model.AgentResourceSetting;
import org.jumpmind.symmetric.is.core.model.AgentSetting;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ComponentEntitySetting;
import org.jumpmind.symmetric.is.core.model.ComponentName;
import org.jumpmind.symmetric.is.core.model.ComponentSetting;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowName;
import org.jumpmind.symmetric.is.core.model.FlowParameter;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderName;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Group;
import org.jumpmind.symmetric.is.core.model.GroupPrivilege;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.ModelName;
import org.jumpmind.symmetric.is.core.model.Project;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.ResourceName;
import org.jumpmind.symmetric.is.core.model.ResourceSetting;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.model.User;
import org.jumpmind.symmetric.is.core.model.UserGroup;
import org.jumpmind.symmetric.is.core.model.UserSetting;
import org.jumpmind.symmetric.is.core.util.NameValue;

// TODO make methods transactional
abstract class AbstractConfigurationService extends AbstractService implements
        IConfigurationService {

    AbstractConfigurationService(IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
    }

    @Override
    public List<FlowName> findFlowsInProject(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        params.put("deleted", 0);
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
        return find(FlowName.class, params);
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
        ProjectVersion projectVersion = new ProjectVersion(projectVersionId);
        refresh(projectVersion);
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
    public List<Agent> findAgentsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        String folderId = null;
        if (folder != null) {
            folderId = folder.getId();
        }
        params.put("folderId", folderId);
        params.put("deleted", 0);
        return findAgents(params, folder);
    }

    @Override
    public List<Agent> findAgentsForHost(String hostName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("host", hostName);
        params.put("deleted", 0);
        return findAgents(params);
    }

    public List<Agent> findAgents() {
        return persistenceManager.find(Agent.class, null, null, tableName(Agent.class));
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
            agent.setFolder(folderMapById.get(agent.getFolderId()));
            refreshAgentSettings(agent);
            refreshAgentResourceSettings(agent);
            refreshAgentDeployments(agent);
        }

        Collections.sort(list, new Comparator<Agent>() {
            @Override
            public int compare(Agent o1, Agent o2) {
                return o1.getCreateTime().compareTo(o2.getCreateTime());
            }
        });
        return list;
    }

    protected void refreshAgentSettings(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        List<AgentSetting> settings = persistenceManager.find(AgentSetting.class, settingParams,
                null, null, tableName(AgentSetting.class));
        agent.setSettings(settings);
    }

    protected void refreshAgentResourceSettings(Agent agent) {
        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("agentId", agent.getId());
        List<AgentResourceSetting> settings = persistenceManager.find(AgentResourceSetting.class,
                settingParams, null, null, tableName(AgentResourceSetting.class));
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

    protected Component findComponent(String id) {
        Component component = new Component();
        component.setId(id);
        refresh(component);
        return component;
    }

    @Override
    public void refresh(ProjectVersion projectVersion) {
        refresh((AbstractObject) projectVersion);
        refresh((AbstractObject) projectVersion.getProject());
    }

    @Override
    public void refresh(Component component) {

        persistenceManager.refresh(component, null, null, tableName(Component.class));

        if (isNotBlank(component.getInputModelId())) {
            component.setInputModel(findModel(component.getInputModelId()));
        }
        if (isNotBlank(component.getOutputModelId())) {
            component.setOutputModel(findModel(component.getOutputModelId()));
        }

        List<ComponentSetting> settings = find(ComponentSetting.class, new NameValue("componentId",
                component.getId()));
        component.setSettings(settings);

        List<ComponentEntitySetting> entitySettings = find(ComponentEntitySetting.class,
                new NameValue("componentId", component.getId()));
        component.setEntitySettings(entitySettings);

        List<ComponentAttributeSetting> attributeSettings = find(ComponentAttributeSetting.class,
                new NameValue("componentId", component.getId()));
        component.setAttributeSettings(attributeSettings);

        component.setResource(findResource(component.getResourceId()));

    }

    @Override
    public Model findModel(String id) {
        Model model = new Model(id);
        refresh(model);
        return model;
    }

    protected Model refreshModelRelations(Model model) {
        model.getModelEntities().clear();
        Map<String, Object> versionParams = new HashMap<String, Object>();
        versionParams.put("modelId", model.getId());
        List<ModelEntity> entities = persistenceManager.find(ModelEntity.class, versionParams,
                null, null, tableName(ModelEntity.class));
        AbstractObjectNameBasedSorter.sort(entities);
        for (ModelEntity entity : entities) {
            refresh(entity);
            model.getModelEntities().add(entity);
        }
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
        refreshAgentSettings(agent);
        refreshAgentResourceSettings(agent);
        refreshAgentDeployments(agent);
    }

    @Override
    public void refresh(Flow flow) {
        refresh((AbstractObject) flow);
        refreshFlowRelations(flow);
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
        for (FlowStep step : steps) {
            step.setComponent(findComponent(step.getComponentId()));
            flow.getFlowSteps().add(step);

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
        for (ComponentAttributeSetting componentAttributeSetting : aSettings) {
            save(componentAttributeSetting);
        }

        List<ComponentEntitySetting> eSettings = component.getEntitySettings();
        for (ComponentEntitySetting componentEntitySetting : eSettings) {
            save(componentEntitySetting);
        }

        List<Setting> settings = component.getSettings();
        for (Setting setting : settings) {
            save(setting);
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
    public void refresh(ModelEntity modelEntity) {
        refresh((AbstractObject) modelEntity);
        Map<String, Object> entityParams = new HashMap<String, Object>();
        entityParams.put("entityId", modelEntity.getId());
        modelEntity.getModelAttributes().clear();
        List<ModelAttribute> attributes = persistenceManager.find(ModelAttribute.class,
                entityParams, null, null, tableName(ModelAttribute.class));
        for (ModelAttribute attribute : attributes) {
            refresh(attribute);
            modelEntity.addModelAttribute(attribute);
        }
    }

    @Override
    public void refresh(ModelAttribute modelAttribute) {
        refresh((AbstractObject) modelAttribute);
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

}
