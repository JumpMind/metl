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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.sql.mapper.StringMapper;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectCreateTimeDescSorter;
import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.Agent;
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
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDefinitionPlugin;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.ReleasePackageProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.ResourceSetting;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.Version;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.util.NameValue;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.util.FormatUtils;

public class ConfigurationService extends AbstractService
        implements IConfigurationService {
    
    protected IOperationsService operationsService;
    protected IDatabasePlatform databasePlatform;

    public ConfigurationService(IOperationsService operationsService, ISecurityService securityService, IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(securityService, persistenceManager, tablePrefix);
        this.operationsService = operationsService;
        this.databasePlatform = databasePlatform;
    }

    @Override
    public List<ProjectVersionDependency> findProjectDependencies(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectVersionId", projectVersionId);
        List<ProjectVersionDependency> list = find(ProjectVersionDependency.class, params,
                ProjectVersionDependency.class);
        for (ProjectVersionDependency projectVersionDependency : list) {
            projectVersionDependency.setTargetProjectVersion(
                    findProjectVersion(projectVersionDependency.getTargetProjectVersionId()));
        }
        return list;
    }
    
    @Override
    public List<ProjectVersionDependency> findProjectDependenciesThatTarget(String projectVersionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("targetProjectVersionId", projectVersionId);
        List<ProjectVersionDependency> list = find(ProjectVersionDependency.class, params,
                ProjectVersionDependency.class);
        for (ProjectVersionDependency projectVersionDependency : list) {
            projectVersionDependency.setTargetProjectVersion(
                    findProjectVersion(projectVersionDependency.getTargetProjectVersionId()));
        }
        return list;
    }

    @Override
    public List<FlowName> findFlowsInProject(String projectVersionId, boolean test) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("deleted", 0);
        params.put("projectVersionId", projectVersionId);
        params.put("test", test ? 1 : 0);
        return find(FlowName.class, params, Flow.class);
    }

    @Override
    public List<ModelName> findModelsInProject(String projectVersionId) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("deleted", 0);
        params.put("projectVersionId", projectVersionId);
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
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("deleted", 0);
        params.put("projectVersionId", projectVersionId);
        return find(ResourceName.class, params, Resource.class);
    }

//    @Override
//    public List<ComponentName> findSharedComponentsInProject(String projectVersionId) {
//        Map<String, Object> params = new HashMap<String, Object>();
//        params.put("projectVersionId", projectVersionId);
//        params.put("deleted", 0);
//        params.put("shared", 1);
//        return find(ComponentName.class, params, Component.class);
//    }

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
    public FlowName findFlowName(String id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        return findOne(FlowName.class, params);
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
        ArrayList<Folder> allFolders = new ArrayList<Folder>(
                foldersById(projectVersionId, type).values());
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

    @Override
    public List<FlowName> findFlows() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deleted", 0);
        List<FlowName> flows = find(FlowName.class, params);
        AbstractObjectNameBasedSorter.sort(flows);
        return flows;
    }

    @Override
    public List<Resource> findResourcesByName(String projectVersionId, String name) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deleted", 0);
        params.put("name", name);
        params.put("projectVersionId", projectVersionId);
        List<Resource> resources = find(Resource.class, params);
        return resources;
    }

    @Override
    public List<Flow> findFlowsByName(String projectVersionId, String name) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deleted", 0);
        params.put("name", name);
        params.put("projectVersionId", projectVersionId);
        List<Flow> flows = find(Flow.class, params);
        return flows;
    }

    @Override
    public List<Model> findModelsByName(String projectVersionId, String name) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deleted", 0);
        params.put("name", name);
        params.put("projectVersionId", projectVersionId);
        List<Model> models = find(Model.class, params);
        return models;
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
        ProjectVersion projectVersion = findOne(ProjectVersion.class, params);
        if (projectVersion != null) {
            refresh(projectVersion);
        }
        return projectVersion;
    }

    @Override
    public List<ProjectVersionDefinitionPlugin> findProjectVersionComponentPlugins(
            String projectVersionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectVersionId", projectVersionId);
        return find(ProjectVersionDefinitionPlugin.class, params);
    }

    @Override
    public void refresh(Project project) {
        persistenceManager.refresh(project, null, null, tableName(Project.class));
        Map<String, Object> params = new HashMap<>();
        params.put("deleted", 0);
        params.put("projectId", project.getId());
        List<ProjectVersion> versions = persistenceManager.find(ProjectVersion.class, params, null,
                null, tableName(ProjectVersion.class));
        project.setProjectVersions(versions);
        for (ProjectVersion projectVersion : versions) {
            projectVersion.setProject(project);
        }
    }

    @Override
    public Map<String, ProjectVersion> findProjectVersions() {
        Map<String, ProjectVersion> projectVersionMap = new HashMap<>();
        List<Project> projects = findProjects();
        for (Project project : projects) {
            List<ProjectVersion> projectVersions = project.getProjectVersions();
            for (ProjectVersion projectVersion : projectVersions) {
                projectVersionMap.put(projectVersion.getId(), projectVersion);
            }
        }
        return projectVersionMap;
    }

    @Override
    public List<Project> findProjects() {
        List<Project> list = persistenceManager.find(Project.class, new NameValue("deleted", 0),
                null, null, tableName(Project.class));
        AbstractObjectNameBasedSorter.sort(list);
        
        for (Project project : list) {
            project.setProjectVersions(findProjectVersionsByProject(project.getId()));    
        }
        return list;
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
    public void refresh(Resource resource) {
        refresh((AbstractObject) resource);

        Map<String, Object> folderParams = new HashMap<String, Object>();
        folderParams.put("id", resource.getFolderId());
        resource.setFolder(findOne(Folder.class, folderParams));

        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("resourceId", resource.getId());
        List<? extends Setting> settings = findSettings(ResourceSetting.class, settingParams);
        resource.setSettings(settings);
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

        @SuppressWarnings("unchecked")
        List<ComponentSetting> settings = (List<ComponentSetting>) findSettings(
                ComponentSetting.class, new NameValue("componentId", component.getId()));
        component.setSettings(settings);

        @SuppressWarnings("unchecked")
        List<ComponentEntitySetting> entitySettings = (List<ComponentEntitySetting>) findSettings(
                ComponentEntitySetting.class, new NameValue("componentId", component.getId()));
        component.setEntitySettings(entitySettings);

        @SuppressWarnings("unchecked")
        List<ComponentAttributeSetting> attributeSettings = (List<ComponentAttributeSetting>) findSettings(
                ComponentAttributeSetting.class, new NameValue("componentId", component.getId()));
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

    protected Model refreshModelRelations(Model model) {
        model.setModelEntities(new ArrayList<>());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("modelId", model.getId());
        List<ModelEntity> entities = persistenceManager.find(ModelEntity.class, params, null, null,
                tableName(ModelEntity.class));
        List<ModelAttribute> attributes = findAllAttributesForModel(model.getId());
        Map<String, ModelEntity> byModelEntityId = new HashMap<String, ModelEntity>();
        for (ModelEntity entity : entities) {
            byModelEntityId.put(entity.getId(), entity);
            model.getModelEntities().add(entity);
        }

        for (ModelAttribute modelAttribute : attributes) {
            byModelEntityId.get(modelAttribute.getEntityId()).getModelAttributes()
                    .add(modelAttribute);
        }

        for (ModelEntity entity : entities) {
            Collections.sort(entity.getModelAttributes());
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
            @SuppressWarnings("unchecked")
            List<ResourceSetting> settings = (List<ResourceSetting>) findSettings(
                    ResourceSetting.class, settingParams);
            resource.setSettings(settings);
            list.add(resource);
        }
        return list;
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

    protected void refresh(Flow flow) {
        refresh((AbstractObject) flow);
        refreshFlowRelations(flow);
    }

    private void refreshFlowRelations(Flow flow) {
        flow.setFlowSteps(new ArrayList<>());
        flow.setFlowStepLinks(new ArrayList<>());
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
        save(flowStep, false);
    }

    protected void save(FlowStep flowStep, boolean newProjectVersion) {
        Component component = flowStep.getComponent();
        save(component);
        save((AbstractObject) flowStep);
    }

    @Override
    public void save(Flow flow) {
        save(flow, false);
    }

    protected void save(Flow flow, boolean newProjectVersion) {
        save((AbstractObject) flow);

        List<FlowStep> flowSteps = flow.getFlowSteps();
        for (FlowStep flowStep : flowSteps) {
            save(flowStep, newProjectVersion);
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
    public void save(ProjectVersionDefinitionPlugin projectVersionComponentPlugin) {
        projectVersionComponentPlugin.setLastUpdateTime(new Date());
        persistenceManager.save(projectVersionComponentPlugin, null, null,
                tableName(projectVersionComponentPlugin.getClass()));
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
    public String getLastKnownVersion() {
        if (doesTableExist(Version.class)) {
            List<Version> versions = persistenceManager.find(Version.class, null, null,
                    tableName(Version.class));
            AbstractObjectCreateTimeDescSorter.sort(versions);
            return versions.size() > 0 ? versions.get(0).getName() : null;
        } else {
            return null;
        }
    }

    @Override
    public ProjectVersion saveNewVersion(String newVersionLabel, ProjectVersion original, String projectVersionType) {
        Map<String, AbstractObject> oldToNewUUIDMapping = new HashMap<>();
        ProjectVersion newVersion = copyWithNewUUID(oldToNewUUIDMapping, original);
        newVersion.setVersionLabel(newVersionLabel);
        newVersion.setVersionType(projectVersionType);
        newVersion.setDeleted(false);
        newVersion.setArchived(false);
        newVersion.setCreateTime(new Date());
        newVersion.setOrigVersionId(original.getId());
        save(newVersion);

        List<ProjectVersionDependency> dependencies = findProjectDependencies(original.getId());
        for (ProjectVersionDependency origProjectVersionDependency : dependencies) {
            ProjectVersionDependency newDependency = copyWithNewUUID(oldToNewUUIDMapping,
                    origProjectVersionDependency);
            newDependency.setProjectVersionId(newVersion.getId());
            save(newDependency);
        }

        List<ModelName> models = findModelsInProject(original.getId());
        for (ModelName modelName : models) {
            Model newModel = copy(oldToNewUUIDMapping, findModel(modelName.getId()));
            newModel.setProjectVersionId(newVersion.getId());
            save(newModel);
        }

        List<ResourceName> resources = findResourcesInProject(original.getId());
        for (ResourceName resourceName : resources) {
            Resource newResource = copy(oldToNewUUIDMapping, findResource(resourceName.getId()));
            newResource.setProjectVersionId(newVersion.getId());
            save(newResource);
        }

        List<FlowName> testFlows = findFlowsInProject(original.getId(), true);
        for (FlowName flowName : testFlows) {
            Flow newFlow = copy(oldToNewUUIDMapping, findFlow(flowName.getId()), true);
            newFlow.setProjectVersionId(newVersion.getId());
            save(newFlow, true);
        }

        List<FlowName> flows = findFlowsInProject(original.getId(), false);
        for (FlowName flowName : flows) {
            Flow newFlow = copy(oldToNewUUIDMapping, findFlow(flowName.getId()), true);
            newFlow.setProjectVersionId(newVersion.getId());
            save(newFlow);
        }

        List<ProjectVersionDefinitionPlugin> projectVersionComponentPlugins = findProjectVersionComponentPlugins(
                original.getId());
        for (ProjectVersionDefinitionPlugin projectVersionComponentPlugin : projectVersionComponentPlugins) {
            projectVersionComponentPlugin.setProjectVersionId(newVersion.getId());
            save(projectVersionComponentPlugin);
        }

        return newVersion;
    }

    @Override
    public Flow copy(Flow original) {
        Map<String, AbstractObject> oldToNewUUIDMapping = new HashMap<>();
        return copy(oldToNewUUIDMapping, original, false);
    }

    @Override
    public Flow copy(Map<String, AbstractObject> oldToNewUUIDMapping, Flow original,
            boolean newProjectVersion) {
        Flow newFlow = copyWithNewUUID(oldToNewUUIDMapping, original);
        newFlow.setFlowParameters(new ArrayList<FlowParameter>());
        newFlow.setFlowStepLinks(new ArrayList<FlowStepLink>());
        newFlow.setFlowSteps(new ArrayList<FlowStep>());

        Map<String, String> oldToNewFlowStepIds = new HashMap<String, String>();
        for (FlowStep flowStep : original.getFlowSteps()) {
            String oldId = flowStep.getId();
            flowStep = copy(oldToNewUUIDMapping, flowStep, newProjectVersion);
            oldToNewFlowStepIds.put(oldId, flowStep.getId());
            flowStep.setFlowId(newFlow.getId());
            newFlow.getFlowSteps().add(flowStep);
        }

        for (FlowStepLink flowStepLink : original.getFlowStepLinks()) {
            String oldSourceStepId = flowStepLink.getSourceStepId();
            String oldTargetStepId = flowStepLink.getTargetStepId();
            flowStepLink = copyWithNewUUID(oldToNewUUIDMapping, flowStepLink);
            flowStepLink.setSourceStepId(oldToNewFlowStepIds.get(oldSourceStepId));
            flowStepLink.setTargetStepId(oldToNewFlowStepIds.get(oldTargetStepId));
            newFlow.getFlowStepLinks().add(flowStepLink);
        }

        for (FlowParameter flowParameter : original.getFlowParameters()) {
            flowParameter = copyWithNewUUID(oldToNewUUIDMapping, flowParameter);
            flowParameter.setFlowId(newFlow.getId());
            newFlow.getFlowParameters().add(flowParameter);
        }

        for (FlowStep flowStep : newFlow.getFlowSteps()) {
            massageValues(oldToNewUUIDMapping, flowStep.getComponent().getSettings());
            massageValues(oldToNewUUIDMapping, flowStep.getComponent().getAttributeSettings());
            massageValues(oldToNewUUIDMapping, flowStep.getComponent().getEntitySettings());

            /**
             * This step should only get a match if we are copying an entire
             * project version because the model attributes will have been
             * copied as well
             */
            for (ComponentAttributeSetting setting : flowStep.getComponent()
                    .getAttributeSettings()) {
                AbstractObject obj = oldToNewUUIDMapping.get(setting.getAttributeId());
                if (obj != null) {
                    setting.setAttributeId(obj.getId());
                }
            }

            /**
             * This step should only get a match if we are copying an entire
             * project version because the model entities will have been copied
             * as well
             */
            for (ComponentEntitySetting setting : flowStep.getComponent().getEntitySettings()) {
                AbstractObject obj = oldToNewUUIDMapping.get(setting.getEntityId());
                if (obj != null) {
                    setting.setEntityId(obj.getId());
                }
            }
        }

        return newFlow;

    }

    @Override
    public FlowStep copy(FlowStep original) {
        return copy(new HashMap<>(), original, false);
    }

    @Override
    public Model copy(Model original) {
        return copy(new HashMap<>(), original);
    }

    @Override
    public Resource copy(Resource original) {
        return copy(new HashMap<>(), original);
    }

    @Override
    public Resource copy(Map<String, AbstractObject> oldToNewUUIDMapping, Resource original) {
        Resource newResource = copyWithNewUUID(oldToNewUUIDMapping, original);
        newResource.setSettings(new ArrayList<>());
        for (Setting setting : original.getSettings()) {
            ResourceSetting cSetting = (ResourceSetting) copyWithNewUUID(oldToNewUUIDMapping,
                    setting);
            cSetting.setResourceId(newResource.getId());
            newResource.getSettings().add(cSetting);
        }
        return newResource;
    }

    @Override
    public Model copy(Map<String, AbstractObject> oldToNewUUIDMapping, Model original) {
        Model newModel = copyWithNewUUID(oldToNewUUIDMapping, original);
        newModel.setModelEntities(new ArrayList<>());
        for (ModelEntity originalModelEntity : original.getModelEntities()) {
            ModelEntity newModelEntity = copyWithNewUUID(oldToNewUUIDMapping, originalModelEntity);
            // TODO: do we really need this put here as it should be done with
            // the copyWithNewUUID above...
            oldToNewUUIDMapping.put(originalModelEntity.getId(), newModelEntity);
            newModelEntity.setModelId(newModel.getId());
            newModelEntity.setModelAttributes(new ArrayList<>());
            for (ModelAttribute originalAttribute : originalModelEntity.getModelAttributes()) {
                ModelAttribute newAttribute = copyWithNewUUID(oldToNewUUIDMapping,
                        originalAttribute);
                newAttribute.setEntityId(newModelEntity.getId());
                newModelEntity.addModelAttribute(newAttribute);
            }
            newModel.getModelEntities().add(newModelEntity);
        }

        for (ModelEntity modelEntity : newModel.getModelEntities()) {
            List<ModelAttribute> attributes = modelEntity.getModelAttributes();
            for (ModelAttribute modelAttribute : attributes) {
                AbstractObject obj = oldToNewUUIDMapping.get(modelAttribute.getTypeEntityId());
                if (obj != null) {
                    modelAttribute.setTypeEntityId(obj.getId());
                }
            }
        }
        return newModel;
    }

    protected void massageValues(Map<String, AbstractObject> oldToNewUUIDMapping,
            List<? extends Setting> settings) {
        Map<String, String> tokens = toStringTokens(oldToNewUUIDMapping);
        for (Setting setting : settings) {
            setting.setValue(FormatUtils.replaceTokens(setting.getValue(), tokens, false));
        }
    }

    protected Map<String, String> toStringTokens(Map<String, AbstractObject> oldToNewUUIDMapping) {
        Map<String, String> oldToNew = new HashMap<>();
        for (String old : oldToNewUUIDMapping.keySet()) {
            oldToNew.put(old, oldToNewUUIDMapping.get(old).getId());
        }
        return oldToNew;
    }

    protected FlowStep copy(Map<String, AbstractObject> oldToNewUUIDMapping, FlowStep original,
            boolean newProjectVersion) {
        FlowStep flowStep = copyWithNewUUID(oldToNewUUIDMapping, original);
        Component component = original.getComponent();
        if (!component.isShared()) {
            component = copy(oldToNewUUIDMapping, component);
        } else if (newProjectVersion) {
            Component newComponent = (Component) oldToNewUUIDMapping.get(component.getId());
            if (newComponent != null) {
                component = newComponent;
            } else {
                component = copy(oldToNewUUIDMapping, component);
            }
        }
        flowStep.setComponent(component);
        return flowStep;
    }

    protected Component copy(Map<String, AbstractObject> oldToNewUUIDMapping, Component original) {
        Component component = copyWithNewUUID(oldToNewUUIDMapping, original);
        AbstractObject obj = oldToNewUUIDMapping.get(original.getInputModelId());
        if (obj != null) {
            component.setInputModelId(obj.getId());
        }

        obj = oldToNewUUIDMapping.get(original.getOutputModelId());
        if (obj != null) {
            component.setOutputModelId(obj.getId());
        }

        obj = oldToNewUUIDMapping.get(original.getResourceId());
        if (obj != null) {
            component.setResourceId(obj.getId());
        }

        component.setEntitySettings(new ArrayList<ComponentEntitySetting>());
        component.setAttributeSettings(new ArrayList<ComponentAttributeSetting>());
        component.setSettings(new ArrayList<Setting>());

        for (Setting setting : original.getSettings()) {
            ComponentSetting cSetting = (ComponentSetting) copyWithNewUUID(oldToNewUUIDMapping,
                    setting);
            cSetting.setComponentId(component.getId());
            component.getSettings().add(cSetting);
        }

        for (ComponentAttributeSetting setting : original.getAttributeSettings()) {
            setting = (ComponentAttributeSetting) copyWithNewUUID(oldToNewUUIDMapping, setting);
            setting.setComponentId(component.getId());
            component.getAttributeSettings().add(setting);
        }

        for (ComponentEntitySetting setting : original.getEntitySettings()) {
            setting = (ComponentEntitySetting) copyWithNewUUID(oldToNewUUIDMapping, setting);
            setting.setComponentId(component.getId());
            component.getEntitySettings().add(setting);
        }

        return component;
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractObject> T copyWithNewUUID(
            Map<String, AbstractObject> oldToNewUUIDMapping, T original) {
        T copy = (T) original.clone();
        copy.setId(UUID.randomUUID().toString());
        oldToNewUUIDMapping.put(original.getId(), copy);
        return copy;
    }

    @Override
    public Resource findPreviousVersionResource(Resource currentResource) {
        Resource previousResource = null;
        ProjectVersion version = findProjectVersion(currentResource.getProjectVersionId());
        if (isNotBlank(version.getOrigVersionId())) {
            Map<String, Object> params = new HashMap<>();
            params.put("rowId", currentResource.getRowId());
            params.put("projectVersionId", version.getOrigVersionId());
            ResourceName name = findOne(ResourceName.class, params, Resource.class);
            if (name != null) {
                previousResource = findResource(name.getId());
            }
        }
        return previousResource;
    }

    @Override
    public List<ReleasePackage> findReleasePackages() {
        // TODO this should really be ReleasePackageName as it won't be fully
        // refreshed with ReleasePackageProjectVersion
        Map<String, Object> params = new HashMap<String, Object>();
        List<ReleasePackage> releasePackages = find(ReleasePackage.class, params);
        AbstractObjectNameBasedSorter.sort(releasePackages);
        return releasePackages;
    }

    @Override
    public ReleasePackage findReleasePackage(String releasePackageId) {
        ReleasePackage releasePackage = findOne(ReleasePackage.class,
                new NameValue("id", releasePackageId));
        releasePackage.setProjectVersions(findReleasePackageProjectVersions(releasePackageId));
        return releasePackage;
    }

    @Override
    public List<ReleasePackageProjectVersion> findReleasePackageProjectVersions(
            String releasePackageId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("releasePackageId", releasePackageId);
        List<ReleasePackageProjectVersion> rppvs = persistenceManager.find(
                ReleasePackageProjectVersion.class, params, null, null,
                tableName(ReleasePackageProjectVersion.class));
        return rppvs;
    }

    @Override
    public void refresh(ReleasePackage releasePackage) {
        refresh((AbstractObject) releasePackage);
        releasePackage
                .setProjectVersions(findReleasePackageProjectVersions(releasePackage.getId()));
    }
    
    @Override
    public void doInBackground() {
    }

    @Override
    public boolean isInstalled() {
        return databasePlatform.getTableFromCache(tableName(Component.class), false) != null;
    }

    @Override
    public boolean isDeployed(Flow flow) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.queryForInt(String.format("select count(*) from %1$s_agent_deployment where flow_id = ? ", tablePrefix),
                flow.getId()) > 0;
    }

    @Override
    public List<String> findAllProjectVersionIds() {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.query(String.format("select id from %1$s_project_version where deleted=0", tablePrefix), new StringMapper());
    }

    protected List<ModelAttribute> findAllAttributesForModel(String modelId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        String sql = String.format(
                "select * from %1$s_model_attribute where entity_id in (select id from %1$s_model_entity where model_id=?)", tablePrefix);
        return template.query(sql, new ISqlRowMapper<ModelAttribute>() {
            @Override
            public ModelAttribute mapRow(Row row) {
                return persistenceManager.map(row, ModelAttribute.class, null, null, tableName(ModelAttribute.class));
            }
        }, new Object[] { modelId });
    }

    protected String getComponentIds(Flow flow) {
        StringBuilder componentIds = new StringBuilder();
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> results = template.query(String.format(
                "SELECT ID, SHARED FROM %1$s_COMPONENT WHERE ID IN (SELECT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%2$s')",
                tablePrefix, flow.getId()));
        for (Row row : results) {
            componentIds.append("'");
            componentIds.append(row.get("ID"));
            componentIds.append("'");
            componentIds.append(",");
            if (row.getString("SHARED").equals("1")) {
                throw new UnsupportedOperationException("Cannot export flows that utilize shared components");
            }
        }
        componentIds.deleteCharAt(componentIds.length() - 1);
        return componentIds.toString();
    }

    private boolean doesTableExist(Class<?> clazz) {
        return databasePlatform.getTableFromCache(tableName(clazz), false) != null;
    }

    @Override
    public List<Component> findDependentSharedComponents(String flowId) {
        List<Component> sharedComponents = new ArrayList<Component>();
        final String SHARED_COMPONENTS_BY_FLOW_SQL = "select distinct c.id from %1$s_flow_step fs inner join %1$s_component c on fs.component_id = c.id where fs.flow_id = '%2$s' and c.shared=1";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(SHARED_COMPONENTS_BY_FLOW_SQL, tablePrefix, flowId));
        for (Row row : ids) {
            sharedComponents.add(this.findComponent(row.getString("id"), false));
        }
        return sharedComponents;
    }

    @Override
    public List<Resource> findDependentResources(String flowId) {

        List<Resource> resources = new ArrayList<Resource>();
        final String RESOURCES_BY_FLOW_SQL = "select distinct c.resource_id from %1$s_flow_step fs inner join %1$s_component c on fs.component_id = c.id where fs.flow_id = '%2$s' and resource_id is not null " +
                "union select distinct cs.value from metl_flow_step fs inner join metl_component c on fs.component_id = c.id inner join metl_component_setting cs on cs.component_id = c.id where fs.flow_id = '%2$s' " +
                "and cs.name in ('source.resource','target.resource')";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(RESOURCES_BY_FLOW_SQL, tablePrefix, flowId));
        for (Row row : ids) {
            resources.add(this.findResource(row.getString("resource_id")));
        }
        return resources;
    }

    @Override
    public List<Model> findDependentModels(String flowId) {
        List<Model> models = new ArrayList<Model>();
        final String MODELS_BY_FLOW_SQL = "select distinct model_id from  "
                + "(select distinct output_model_id as model_id from %1$s_flow_step fs inner join %1$s_component c on fs.component_id = c.id where fs.flow_id = '%2$s' and output_model_id is not null union "
                + " select distinct input_model_id as model_id from %1$s_flow_step fs inner join %1$s_component c on fs.component_id = c.id where fs.flow_id = '%2$s' and input_model_id is not null)";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(MODELS_BY_FLOW_SQL, tablePrefix, flowId));
        for (Row row : ids) {
            models.add(this.findModel(row.getString("model_id")));
        }
        return models;
    }

    @Override
    public List<Flow> findDependentFlows(String projectVersionId) {
        List<Flow> flows = new ArrayList<Flow>();
        final String FLOWS_BY_PROJECT_SQL = "select distinct id from %1$s_flow where project_version_id =  '%2$s'";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(FLOWS_BY_PROJECT_SQL, tablePrefix, projectVersionId));
        for (Row row : ids) {
            flows.add(this.findFlow(row.getString("id")));
        }
        return flows;
    }

    @Override
    public List<Flow> findAffectedFlowsByFlow(String flowId) {
        List<Flow> flows = new ArrayList<Flow>();

        final String AFFECTED_FLOWS_BY_FLOW_SQL = "select distinct flow_id from %1$s_flow_step fs inner join %1$s_component c on fs.component_id = c.id "
                + "inner join %1$s_component_setting cs on cs.component_id = c.id " + "where cs.name='flow.id' and cs.value = '%2$s'";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(AFFECTED_FLOWS_BY_FLOW_SQL, tablePrefix, flowId));
        for (Row row : ids) {
            flows.add(this.findFlow(row.getString("flow_id")));
        }

        return flows;
    }

    @Override
    public List<Flow> findAffectedFlowsByResource(String resourceId) {
        List<Flow> flows = new ArrayList<Flow>();

        final String AFFECTED_FLOWS_BY_RESOURCE_SQL = "select distinct flow_id from %1$s_flow_step fs inner join %1$s_component c on fs.component_id = c.id "
                + "where c.resource_id = '%2$s'";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(AFFECTED_FLOWS_BY_RESOURCE_SQL, tablePrefix, resourceId));
        for (Row row : ids) {
            flows.add(this.findFlow(row.getString("flow_id")));
        }

        return flows;
    }

    @Override
    public List<Flow> findAffectedFlowsByModel(String modelId) {
        List<Flow> flows = new ArrayList<Flow>();

        final String AFFECTED_FLOWS_BY_MODEL_SQL = "select distinct flow_id from %1$s_flow_step fs inner join %1$s_component c on fs.component_id = c.id "
                + "where c.input_model_id = '%2$s' or c.output_model_id = '%2$s'";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(AFFECTED_FLOWS_BY_MODEL_SQL, tablePrefix, modelId));
        for (Row row : ids) {
            flows.add(this.findFlow(row.getString("flow_id")));
        }
        return flows;
    }

    @Override
    public void deleteReleasePackageProjectVersionsForReleasePackage(String releasePackageId) {
        final String DELETE_RELEASE_PACKAGE_VERSIONS_FOR_RELEASE_PACKAGE = "delete from %1$s_release_package_project_version " +
                "where release_package_id = '%2$s'";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        template.update(String.format(DELETE_RELEASE_PACKAGE_VERSIONS_FOR_RELEASE_PACKAGE, tablePrefix, releasePackageId));                
    }

    @Override
    public List<ProjectVersion> findProjectVersionsByProject(String projectId) {        
        Map<String, Object> params = new HashMap<>();
        params.put("deleted", 0);
        params.put("projectId", projectId);
        List<ProjectVersion> versions = persistenceManager.find(ProjectVersion.class, params, null,
                null, tableName(ProjectVersion.class));
        AbstractObjectCreateTimeDescSorter.sort(versions);
        return versions;
    }
    
    @Override
    public void updateProjectVersionDependency(ProjectVersionDependency dependency, String newTargetProjectVersionId) {
        //TODO: audit events
        Map<String, String> oldToNewResourceIdMap = getOldToNewResourceIdMap(dependency, newTargetProjectVersionId);
        Map<String, String> oldToNewModelIdMap = getOldToNewModelIdMap(dependency, newTargetProjectVersionId);
        Map<String, String> oldToNewModelEntityIdMap = getOldToNewModelEntityIdMap(oldToNewModelIdMap);
        Map<String, String> oldToNewModelAttributeIdMap = getOldToNewModelAttributeIdMap(oldToNewModelEntityIdMap);

        ISqlTransaction transaction = databasePlatform.getSqlTemplate().startSqlTransaction();
        try {        
            updateProjectVersionWithNewDependencyGUIDs(oldToNewResourceIdMap, oldToNewModelIdMap,
                    oldToNewModelEntityIdMap, oldToNewModelAttributeIdMap, newTargetProjectVersionId,
                    transaction);
            updateProjectDependencyWithNewVersion(dependency, newTargetProjectVersionId);
            transaction.commit();
        } catch (Exception e) {
            log.error(String.format("Error updating project version dependencies %s",e.getMessage()));
            transaction.rollback();
        }
    }
    
    private void updateProjectDependencyWithNewVersion(ProjectVersionDependency dependency, String newTargetProjectVersionId) {        
        dependency.setTargetProjectVersionId(newTargetProjectVersionId);
        save(dependency);
    }
    
    private Map<String, String> getOldToNewResourceIdMap(ProjectVersionDependency dependency, String newTargetProjectVersionId) {
        Map<String, String> oldToNewResourceIdMap = new HashMap<String, String>();
        final String RESOURCES_USED_FROM_DEPENDENT_PROJECTS = 
                "select \n" + 
                "   distinct c.resource_id\n" + 
                "   ,nr.id\n" + 
                "from \n" + 
                "   %1$s_component c\n" + 
                "   inner join %1$s_project_version pv\n" + 
                "      on c.project_version_id = pv.id\n" + 
                "   left outer join %1$s_resource cr\n" + 
                "      on cr.id = c.resource_id\n" + 
                "      and cr.project_version_id = pv.id\n" + 
                "   inner join %1$s_project_version_dependency pvd\n" + 
                "      on pvd.project_version_id = pv.id\n" + 
                "   inner join %1$s_resource dr\n" + 
                "      on dr.project_version_id = pvd.target_project_version_id\n" + 
                "      and dr.id = c.resource_id\n" + 
                "   inner join %1$s_resource nr\n" + 
                "      on nr.project_version_id = '%2$s'" + 
                "      and dr.row_id = nr.row_id\n" + 
                "where\n" + 
                "   cr.id is null\n" + 
                "   and pv.id = '%3$s'" + 
                "union\n" + 
                "select\n" + 
                "   distinct cs.value\n" + 
                "   , nr.id\n" + 
                "from\n" + 
                "   %1$s_component_setting cs\n" + 
                "   inner join %1$s_component c\n" + 
                "      on cs.component_id = c.id\n" + 
                "   inner join %1$s_project_version pv\n" + 
                "      on c.project_version_id = pv.id\n" + 
                "   left outer join %1$s_resource r\n" + 
                "      on r.id = c.resource_id\n" + 
                "      and r.project_version_id = pv.id\n" + 
                "   inner join %1$s_project_version_dependency pvd\n" + 
                "      on pvd.project_version_id = pv.id\n" + 
                "   inner join %1$s_resource dr\n" + 
                "      on dr.project_version_id = pvd.target_project_version_id\n" + 
                "      and dr.id = cs.value\n" + 
                "   inner join %1$s_resource nr\n" + 
                "      on nr.project_version_id = '%2$s'" + 
                "      and dr.row_id = nr.row_id\n" + 
                "where\n" + 
                "   cs.name in ('target.resource','source.resource')\n" + 
                "   and r.id is null\n" + 
                "   and pv.id = '%3$s'";
//TODO: come up with every uuid and then check against metl_resource to  ensure that it is a resource or not       
        
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(RESOURCES_USED_FROM_DEPENDENT_PROJECTS, tablePrefix, newTargetProjectVersionId, dependency.getProjectVersionId()));
        for (Row row : ids) {
            oldToNewResourceIdMap.put(row.getString("resource_id"), row.getString("id"));
        }        
        return oldToNewResourceIdMap;
    }

    private Map<String, String> getOldToNewModelIdMap(ProjectVersionDependency dependency, String newTargetProjectVersionId) {
        Map<String, String> oldToNewModelIdMap = new HashMap<String, String>();
        final String MODELS_USED_FROM_DEPENDENT_PROJECTS =
                "select \n" + 
                "   distinct c.input_model_id\n" + 
                "   ,nm.id\n" + 
                "from \n" + 
                "   %1$s_component c\n" + 
                "   inner join %1$s_project_version pv\n" + 
                "      on c.project_version_id = pv.id\n" + 
                "   left outer join %1$s_model cm\n" + 
                "      on cm.id = c.input_model_id\n" + 
                "      and cm.project_version_id = pv.id\n" + 
                "   inner join %1$s_project_version_dependency pvd\n" + 
                "      on pvd.project_version_id = pv.id\n" + 
                "   inner join %1$s_model dm\n" + 
                "      on dm.project_version_id = pvd.target_project_version_id\n" + 
                "      and dm.id = c.input_model_id\n" + 
                "   inner join %1$s_model nm\n" + 
                "      on nm.project_version_id = '%2$s'" + 
                "      and dm.row_id = nm.row_id\n" + 
                "where\n" + 
                "   cm.id is null\n" + 
                "   and pv.id = '%3$s'" + 
                "union\n" + 
                "select \n" + 
                "   distinct c.output_model_id\n" + 
                "   ,nm.id\n" + 
                "from \n" + 
                "   %1$s_component c\n" + 
                "   inner join %1$s_project_version pv\n" + 
                "      on c.project_version_id = pv.id\n" + 
                "   left outer join %1$s_model cm\n" + 
                "      on cm.id = c.output_model_id\n" + 
                "      and cm.project_version_id = pv.id\n" + 
                "   inner join %1$s_project_version_dependency pvd\n" + 
                "      on pvd.project_version_id = pv.id\n" + 
                "   inner join %1$s_model dm\n" + 
                "      on dm.project_version_id = pvd.target_project_version_id\n" + 
                "      and dm.id = c.output_model_id\n" + 
                "   inner join %1$s_model nm\n" + 
                "      on nm.project_version_id = '%2$s'" + 
                "      and dm.row_id = nm.row_id\n" + 
                "where\n" + 
                "   cm.id is null\n" + 
                "   and pv.id = '%3$s' ";
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> ids = template.query(String.format(MODELS_USED_FROM_DEPENDENT_PROJECTS, tablePrefix, newTargetProjectVersionId, dependency.getProjectVersionId()));
        for (Row row : ids) {
            oldToNewModelIdMap.put(row.getString("input_model_id"), row.getString("id"));
        }        
        return oldToNewModelIdMap;
    }

    private Map<String, String> getOldToNewModelEntityIdMap(Map<String, String> oldToNewModelIdMap) {
        Map<String, String> oldToNewModelEntityIdMap = new HashMap<String, String>();
        final String MODEL_ENTITIES_USED_FROM_DEPENDENT_PROJECTS =
                "select \n" + 
                "   ome.id as id\n" + 
                "   ,nme.id as id_1\n" + 
                "from\n" + 
                "   %1$s_model_entity ome\n" + 
                "   left outer join %1$s_model_entity nme\n" + 
                "      on ome.name = nme.name\n" + 
                "where\n" + 
                "   ome.model_id = '%2$s'\n" + 
                "   and nme.model_id = '%3$s'";
        ISqlTemplate template = databasePlatform.getSqlTemplate();

        for (Map.Entry<String,String> entry : oldToNewModelIdMap.entrySet()) {
            List<Row> ids = template.query(String.format(MODEL_ENTITIES_USED_FROM_DEPENDENT_PROJECTS, tablePrefix, entry.getKey(),entry.getValue()));
            for (Row row : ids) {
                oldToNewModelEntityIdMap.put(row.getString("id"), row.getString("id_1"));
            }                    
        }
        return oldToNewModelEntityIdMap;
    }

    private Map<String, String> getOldToNewModelAttributeIdMap(Map<String, String> oldToNewModelEntityIdMap) {
        Map<String, String> oldToNewModelAttributeIdMap = new HashMap<String, String>();
        final String MODEL_ENTITIES_USED_FROM_DEPENDENT_PROJECTS =
                "select \n" + 
                "   oma.id as id\n" + 
                "   ,nma.id as id_1\n" + 
                "from\n" +
                "   %1$s_model_attribute oma\n" + 
                "   left outer join %1$s_model_attribute nma\n" + 
                "      on oma.name = nma.name\n" + 
                "where\n" + 
                "   oma.entity_id = '%2$s'\n" + 
                "   and nma.entity_id = '%3$s'";
        ISqlTemplate template = databasePlatform.getSqlTemplate();

        for (Map.Entry<String,String> entry : oldToNewModelEntityIdMap.entrySet()) {
            List<Row> ids = template.query(String.format(MODEL_ENTITIES_USED_FROM_DEPENDENT_PROJECTS, tablePrefix, entry.getKey(),entry.getValue()));
            for (Row row : ids) {
                oldToNewModelAttributeIdMap.put(row.getString("id"), row.getString("id_1"));
            }                    
        }
        return oldToNewModelAttributeIdMap;
    }
    
    private void updateProjectVersionWithNewDependencyGUIDs(Map<String,String> oldToNewResourceIdMap, Map<String,String> oldToNewModelIdMap,
                Map<String,String> oldToNewModelEntityIdMap, Map<String,String> oldToNewModelAttributeIdMap,
                String targetProjectVersionId, ISqlTransaction transaction) {

        updateProjectVersionWithNewResources(oldToNewResourceIdMap, targetProjectVersionId, transaction);
        updateProjectVersionWithNewModels(oldToNewModelIdMap, oldToNewModelEntityIdMap,
                oldToNewModelAttributeIdMap, targetProjectVersionId, transaction);
    }
    
    private void updateProjectVersionWithNewResources(Map<String, String> oldToNewResourceIdMap, String targetProjectVersionId,
            ISqlTransaction transaction) {

        final String UPDATE_RESOURCE_IN_COMPONENT_OLD_TO_NEW = 
                "update %1$s_component set resource_id='%2$s' where resource_id='%3$s' and project_version_id='%4$s'";
        final String UPDATE_RESOURCE_IN_COMPONENT_SETTING_OLD_TO_NEW = 
                "update %1$s_component_setting as cs\n" + 
                "   set cs.value='%2$s'\n" + 
                "where \n" + 
                "   cs.value='%3$s'\n" + 
                "   and cs.name in ('source.resource','target.resource')\n" + 
                "   and cs.component_id in \n" + 
                "   (\n" + 
                "      select \n" + 
                "         c.id\n" + 
                "      from \n" + 
                "         %1$s_component c\n" + 
                "      where \n" + 
                "         cs.component_id = c.id         \n" + 
                "         and c.project_version_id='%4$s'\n" + 
                "   )\n";
//TODO same comment here as above - look for uuid and then compare against metl_resource              
        for (Map.Entry<String, String> entry : oldToNewResourceIdMap.entrySet()) {
            transaction.execute(String.format(UPDATE_RESOURCE_IN_COMPONENT_OLD_TO_NEW, tablePrefix, entry.getValue(), entry.getKey(), targetProjectVersionId));    
            transaction.execute(String.format(UPDATE_RESOURCE_IN_COMPONENT_SETTING_OLD_TO_NEW, tablePrefix, entry.getValue(), entry.getKey(), targetProjectVersionId));                
        }                
    }
    
    private void updateProjectVersionWithNewModels(Map<String, String> oldToNewModelIdMap, 
            Map<String, String> oldToNewModelEntityIdMap, Map<String, String> oldToNewModelAttributeIdMap,
            String targetProjectVersionId, ISqlTransaction transaction) {
        
        final String UPDATE_INPUT_MODEL_IN_COMPONENT_OLD_TO_NEW = 
                "update %1$s_component set input_model_id='%2$s' where input_model_id='%3$s' and project_version_id='%4$s'";
        final String UPDATE_OUTPUT_MODEL_IN_COMPONENT_OLD_TO_NEW = 
                "update %1$s_component set output_model_id='%2$s' where output_model_id='%3$s' and project_version_id='%4$s'";
              
        for (Map.Entry<String, String> entry : oldToNewModelIdMap.entrySet()) {        
            transaction.execute(String.format(UPDATE_INPUT_MODEL_IN_COMPONENT_OLD_TO_NEW, tablePrefix, entry.getValue(), entry.getKey(), targetProjectVersionId));
            transaction.execute(String.format(UPDATE_OUTPUT_MODEL_IN_COMPONENT_OLD_TO_NEW, tablePrefix, entry.getValue(), entry.getKey(), targetProjectVersionId));            
        }        
        updateProjectVersionWithNewModelEntityIds(oldToNewModelEntityIdMap, targetProjectVersionId, transaction);
        updateProjectVersionWithNewModelAttributeIds(oldToNewModelAttributeIdMap, targetProjectVersionId, transaction);        
    }

    private void updateProjectVersionWithNewModelEntityIds(
            Map<String, String> oldToNewModelEntityIdMap, String targetProjectVersionId, ISqlTransaction transaction) {

        final String UPDATE_COMPONENT_ENTITY_SETTING_OLD_TO_NEW = 
                "update %1$s_component_entity_setting as ces\n" + 
                "   set ces.entity_id='%2$s'\n" + 
                "where \n" + 
                "   ces.entity_id='%3$s'\n" + 
                "   and ces.component_id in \n" + 
                "   (\n" + 
                "      select \n" + 
                "         c.id\n" + 
                "      from \n" + 
                "         %1$s_component c\n" + 
                "      where \n" + 
                "         ces.component_id = c.id         \n" + 
                "         and c.project_version_id='%4$s'\n" + 
                "   )\n";
     
        for (Map.Entry<String, String> entry : oldToNewModelEntityIdMap.entrySet()) {        
            transaction.execute(String.format(UPDATE_COMPONENT_ENTITY_SETTING_OLD_TO_NEW, tablePrefix, 
                    entry.getValue(), entry.getKey(), targetProjectVersionId));                    
        }                
    }   
        
    private void updateProjectVersionWithNewModelAttributeIds(
            Map<String, String> oldToNewModelAttributeIdMap, String targetProjectVersionId, ISqlTransaction transaction) {

        final String UPDATE_COMPONENT_ATTRIBUTE_SETTING_OLD_TO_NEW = 
                "update %1$s_component_attribute_setting as ces\n" + 
                "   set ces.attribute_id='%2$s'\n" + 
                "where \n" + 
                "   ces.attribute_id='%3$s'\n" + 
                "   and ces.component_id in \n" + 
                "   (\n" + 
                "      select \n" + 
                "         c.id\n" + 
                "      from \n" + 
                "         %1$s_component c\n" + 
                "      where \n" + 
                "         ces.component_id = c.id         \n" + 
                "         and c.project_version_id='%4$s'\n" + 
                "   )\n";

        final String UPDATE_MODEL_ATTRIBUTE_IN_COMPONENT_SETTING_OLD_TO_NEW = 
                "update %1$s_component_setting as cs\n" + 
                "   set cs.value='%2$s'\n" + 
                "where \n" + 
                "   cs.value='%3$s'\n" + 
                "   and cs.name in ('lookup.key.attribute','lookup.value.attribute','replacement.key.attribute','replacement.value.attribute','sequence.attribute')\n" + 
                "   and cs.component_id in \n" + 
                "   (\n" + 
                "      select \n" + 
                "         c.id\n" + 
                "      from \n" + 
                "         %1$s_component c\n" + 
                "      where \n" + 
                "         cs.component_id = c.id         \n" + 
                "         and c.project_version_id='%4$s'\n" + 
                "   )\n";
              
        for (Map.Entry<String, String> entry : oldToNewModelAttributeIdMap.entrySet()) {        
            transaction.execute(String.format(UPDATE_COMPONENT_ATTRIBUTE_SETTING_OLD_TO_NEW, tablePrefix, 
                    entry.getValue(), entry.getKey(), targetProjectVersionId));
            transaction.execute(String.format(UPDATE_MODEL_ATTRIBUTE_IN_COMPONENT_SETTING_OLD_TO_NEW, tablePrefix, 
                    entry.getValue(), entry.getKey(), targetProjectVersionId));            
        }                
    }    
}
