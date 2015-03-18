package org.jumpmind.symmetric.is.core.persist;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentSetting;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.model.ComponentVersionAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ComponentVersionEntitySetting;
import org.jumpmind.symmetric.is.core.model.ComponentVersionSetting;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelAttributeRelationship;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.ModelEntityRelationship;
import org.jumpmind.symmetric.is.core.model.ModelVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.ResourceSetting;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.util.NameValue;

// TODO make methods transactional
abstract class AbstractConfigurationService extends AbstractService implements
        IConfigurationService {

    AbstractConfigurationService(IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
    }
    
    @Override
    public FlowVersion findFlowVersion(String id) {
        FlowVersion flowVersion = new FlowVersion();
        flowVersion.setId(id);
        refresh(flowVersion);
        return flowVersion;
    }

    @Override
    public List<Folder> findFolders(FolderType type) {
        Collection<Folder> allFolders = foldersById(type).values();
        List<Folder> rootFolders = new ArrayList<Folder>();
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

    protected Map<String, Folder> foldersById(FolderType type) {
        Map<String, Object> byType = new HashMap<String, Object>();
        byType.put("type", type.name());
        List<Folder> folders = find(Folder.class, byType);

        Map<String, Folder> all = new HashMap<String, Folder>();
        for (Folder folder : folders) {
            all.put(folder.getId(), folder);
        }
        return all;
    }

    @Override
    public List<Flow> findFlowsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        String folderId = null;
        if (folder != null) {
        	folderId = folder.getId();
        }
        params.put("folderId", folderId);
        List<Flow> flows = find(Flow.class, params);
        for (Flow flow : flows) {
            flow.setFolder(folder);
            Map<String, Object> versionParams = new HashMap<String, Object>();
            versionParams.put("flowId", flow.getId());
            List<FlowVersion> versionDatas = find(FlowVersion.class,
                    versionParams);
            for (FlowVersion version : versionDatas) {
                version.setFlow(flow);
                refreshFlowVersionRelations(version);
                flow.getFlowVersions().add(version);
            }
        }
        return flows;
    }

    @Override
    public List<Model> findModelsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("folderId", folder.getId());
        List<Model> models = find(Model.class, params);
        for (Model model : models) {
        	model.setFolder(folder);        	
            Map<String, Object> versionParams = new HashMap<String, Object>();
            versionParams.put("modelId", model.getId());
            List<ModelVersion> modelVersions = find(ModelVersion.class, versionParams);
            for (ModelVersion modelVersion : modelVersions) {
            	modelVersion.setModel(model);
            	refreshModelVersionRelations(modelVersion);
                model.getModelVersions().add(modelVersion);
            }
        }

        return models;
    }
  
    @Override
    public List<Resource> findResourcesInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("folderId", folder.getId());
        List<Resource> datas = find(Resource.class, params);
        return buildResource(folder, datas);
    }

    @Override
    public List<Resource> findResourcesByTypes(String... types) {
        List<Resource> list = new ArrayList<Resource>();
        if (types != null) {
            for (String type : types) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("type", type);
                List<Resource> datas = find(Resource.class, params);
                list = buildResource(null, datas);
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
        return findAgents(params, folder);
    }

    @Override
    public List<Agent> findAgentsForHost(String hostName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("host", hostName);
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
            folderMapById = foldersById(FolderType.RUNTIME);
        }

        for (Agent agent : list) {
            Map<String, Object> settingParams = new HashMap<String, Object>();
            settingParams.put("agentId", agent.getId());
            List<AgentSetting> settings = persistenceManager.find(AgentSetting.class,
                    settingParams, null, null, tableName(AgentSetting.class));
            agent.setFolder(folderMapById.get(agent.getFolderId()));
            agent.setSettings(settings);

            List<AgentDeployment> deployments = persistenceManager.find(AgentDeployment.class,
                    settingParams, null, null, tableName(AgentDeployment.class));
            for (AgentDeployment agentDeployment : deployments) {
                FlowVersion flowVersion = new FlowVersion();
                flowVersion.setId(agentDeployment.getFlowVersionId());
                refresh(flowVersion);
                agentDeployment.setFlowVersion(flowVersion);
                agent.getAgentDeployments().add(agentDeployment);
            }
        }
        return list;
    }

    @Override
    public List<AgentDeployment> findAgentDeploymentsFor(FlowVersion flowVersion) {
        List<AgentDeployment> deployments = persistenceManager.find(AgentDeployment.class,
                new NameValue("flowVersionId", flowVersion.getId()), null, null,
                tableName(AgentDeployment.class));
        for (AgentDeployment deployment : deployments) {
            deployment.setFlowVersion(flowVersion);
        }
        return deployments;
    }

    @Override
    public Resource findResource(String id) {
        Resource resource = findOne(Resource.class, new NameValue("id", id));
        if (resource != null) {
            refresh(resource);
        }
        return resource;
    }

    protected ComponentVersion findComponentVersion(String id) {
        ComponentVersion componentVersion = new ComponentVersion();
        componentVersion.setId(id);
        refresh(componentVersion);
        return componentVersion;
    }

    @Override
    public void refresh(ComponentVersion componentVersion) {

        persistenceManager.refresh(componentVersion, null, null, tableName(ComponentVersion.class));

        componentVersion.setComponent(findOne(Component.class,
                new NameValue("id", componentVersion.getComponentId())));

        if (isNotBlank(componentVersion.getInputModelVersiondId())) {
            componentVersion.setInputModelVersion(findModelVersion(componentVersion
                    .getInputModelVersiondId()));
        }
        if (isNotBlank(componentVersion.getOutputModelVersionId())) {
            componentVersion.setOutputModelVersion(findModelVersion(componentVersion
                    .getOutputModelVersionId()));
        }

        List<ComponentVersionSetting> settings = find(ComponentVersionSetting.class, new NameValue(
                "componentVersionId", componentVersion.getId()));
        componentVersion.setSettings(settings);
        
        List<ComponentVersionEntitySetting> entitySettings = find(ComponentVersionEntitySetting.class,
                new NameValue("componentVersionId", componentVersion.getId()));
        componentVersion.setEntitySettings(entitySettings);
        
        List<ComponentVersionAttributeSetting> attributeSettings = find(ComponentVersionAttributeSetting.class,
                new NameValue("componentVersionId", componentVersion.getId()));
        componentVersion.setAttributeSettings(attributeSettings);
        
        componentVersion.setResource(findResource(componentVersion.getResourceId()));

    }

    protected ModelVersion findModelVersion(String id) {
        ModelVersion modelVersion = new ModelVersion();
        modelVersion.setId(id);
        persistenceManager.refresh(modelVersion, null, null, tableName(ModelVersion.class));
        Model model = new Model();
        model.setId(modelVersion.getId());
        persistenceManager.refresh(model, null, null, tableName(Model.class));
        return refreshModelVersionRelations(modelVersion);
    }

    protected ModelVersion refreshModelVersionRelations(ModelVersion modelVersion) {

        modelVersion.getModelEntities().clear();
        Map<String, Object> versionParams = new HashMap<String, Object>();
        versionParams.put("modelVersionId", modelVersion.getId());
        List<ModelEntity> entities = persistenceManager.find(ModelEntity.class,
                versionParams, null, null, tableName(ModelEntity.class));
        for (ModelEntity entity : entities) {
            refresh(entity);
            modelVersion.getModelEntities().put(entity.getName(), entity);
        }
        return modelVersion;
    }

    protected List<Resource> buildResource(Folder folder, List<Resource> datas) {
        return buildResource(folder, datas.toArray(new Resource[datas.size()]));
    }

    protected List<Resource> buildResource(Folder folder, Resource... resources) {
        List<Resource> list = new ArrayList<Resource>();
        for (Resource resource : resources) {
            if (folder == null) {
                Map<String, Object> folderParams = new HashMap<String, Object>();
                folderParams.put("id", resource.getFolderId());
                folder = findOne(Folder.class, folderParams);
            }

            Map<String, Object> settingParams = new HashMap<String, Object>();
            settingParams.put("resourceId", resource.getId());
            List<ResourceSetting> settings = find(ResourceSetting.class, settingParams);
            resource.setSettings(settings);
            resource.setFolder(folder);
            list.add(resource);
        }
        return list;
    }

    @Override
    public void delete(AgentDeployment agentDeployment) {
        delete((AbstractObject)agentDeployment);
    }

    @Override
    public void delete(FlowVersion flowVersion, FlowStep flowStep) {
        List<FlowStepLink> links = flowVersion
                .removeFlowStepLinks(flowStep.getId());
        for (FlowStepLink link : links) {
            delete(link);
        }

        flowVersion.removeFlowStep(flowStep);
        delete(flowStep);
    }

    @Override
    public void deleteFolder(String folderId) {
        Map<String, Object> byType = new HashMap<String, Object>();
        byType.put("parentFolderId", folderId);
        List<Folder> folderDatas = find(Folder.class, byType);
        for (Folder folderData : folderDatas) {
            deleteFolder(folderData.getId());
        }
        persistenceManager.delete(new Folder(folderId), null, null, tableName(Folder.class));
    }

    @Override
    public void delete(Resource resource) {
        List<Setting> settings = resource.getSettings();
        for (Setting settingData : settings) {
            delete(settingData);
        }
        persistenceManager.delete(resource, null, null, tableName(Resource.class));
    }

    @Override
    public void delete(Agent agent) {
        List<Setting> settings = agent.getSettings();
        for (Setting settingData : settings) {
            delete(settingData);
        }
        persistenceManager.delete(agent, null, null, tableName(Agent.class));
    }

    @Override
    public void delete(Flow flow) {
        List<FlowVersion> versions = flow.getFlowVersions();
        for (FlowVersion flowVersion : versions) {
            deleteFlowVersion(flowVersion);
        }

        persistenceManager.delete(flow, null, null, tableName(Flow.class));

    }

    @Override
    public void delete(FlowStepLink link) {
        persistenceManager.delete(link, null, null, tableName(FlowStepLink.class));
    }

    @Override
    public void deleteFlowVersion(FlowVersion flowVersion) {

        List<FlowStepLink> links = flowVersion.getFlowStepLinks();
        for (FlowStepLink link : links) {
            delete(link);
        }
        List<FlowStep> steps = flowVersion.getFlowSteps();
        for (FlowStep step : steps) {
            delete(step);

            ComponentVersion componentVersion = step.getComponentVersion();
            Component component = componentVersion.getComponent();
            if (!component.isShared()) {
                /*
                 * I do not think there will ever be more than one version of a
                 * non shared component
                 */
                delete(componentVersion);
                delete(component);
            }
        }
        delete(flowVersion);
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
    public void refresh(Model model) {
        refresh((AbstractObject) model);

        Map<String, Object> folderParams = new HashMap<String, Object>();
        folderParams.put("id", model.getFolderId());
        model.setFolder(findOne(Folder.class, folderParams));
    }

    @Override
    public void refresh(Agent agent) {
        refresh((AbstractObject) agent);

        // TODO refresh settings
    }

    @Override
    public void refresh(FlowVersion flowVersion) {
        refresh((AbstractObject) flowVersion);
        refreshFlowVersionRelations(flowVersion);
    }

    private void refreshFlowVersionRelations(FlowVersion flowVersion) {
        Flow flow = flowVersion.getFlow();
        if (flow == null) {
            flow = new Flow(flowVersion.getId());
            flowVersion.setFlow(flow);
        }
        refresh(flow);

        flowVersion.getFlowSteps().clear();
        flowVersion.getFlowStepLinks().clear();
        Map<String, Object> versionParams = new HashMap<String, Object>();
        versionParams.put("flowVersionId", flowVersion.getId());
        List<FlowStep> steps = persistenceManager.find(FlowStep.class,
                versionParams, null, null, tableName(FlowStep.class));
        for (FlowStep step : steps) {
            step.setComponentVersion(findComponentVersion(step.getComponentVersionId()));
            flowVersion.getFlowSteps().add(step);

            Map<String, Object> linkParams = new HashMap<String, Object>();
            linkParams.put("sourceStepId", step.getId());

            List<FlowStepLink> dataLinks = persistenceManager.find(
                    FlowStepLink.class, linkParams, null, null,
                    tableName(FlowStepLink.class));
            for (FlowStepLink dataLink : dataLinks) {
                flowVersion.getFlowStepLinks().add(dataLink);
            }
        }

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
        ComponentVersion version = flowStep.getComponentVersion();
        Component component = version.getComponent();
        if (!component.isShared()) {
            save(component);
            save(version);
        }
        save((AbstractObject) flowStep);
    }

    @Override
    public void save(FlowVersion flowVersion) {

        save((AbstractObject) flowVersion);

        List<FlowStep> flowSteps = flowVersion.getFlowSteps();
        for (FlowStep flowStep : flowSteps) {
            save(flowStep);
        }

        List<FlowStepLink> links = flowVersion.getFlowStepLinks();
        for (FlowStepLink link : links) {
            link.setLastModifyTime(new Date());
            persistenceManager.save(link, null, null, tableName(link.getClass()));
        }

    }

    @Override
    public void delete(Model model) {

        List<ModelVersion> versions = model.getModelVersions();
        for (ModelVersion modelVersion : versions) {
            delete(modelVersion);
        }

        persistenceManager.delete(model, null, null, tableName(Model.class));
    }

    @Override
    public void delete(ModelVersion modelVersion) {

        Iterator<Entry<String, ModelEntity>> itr = modelVersion.getModelEntities().entrySet()
                .iterator();
        while (itr.hasNext()) {
            delete(itr.next().getValue());
        }
        persistenceManager.delete(modelVersion, null, null, tableName(ModelVersion.class));
    }

    @Override
    public void delete(ModelEntity modelEntity) {

        Iterator<ModelEntityRelationship> itrr = modelEntity.getModelEntityRelationships()
                .iterator();
        while (itrr.hasNext()) {
            delete(itrr.next());
        }

        Iterator<Entry<String, ModelAttribute>> itra = modelEntity.getModelAttributes().entrySet()
                .iterator();
        while (itra.hasNext()) {
            delete(itra.next().getValue());
        }

        persistenceManager.delete(modelEntity, null, null, tableName(ModelEntity.class));
    }

    @Override
    public void delete(ModelAttribute modelAttribute) {

        persistenceManager.delete(modelAttribute, null, null, tableName(ModelAttribute.class));
    }

    @Override
    public void delete(ModelEntityRelationship modelEntityRelationship) {

        Iterator<ModelAttributeRelationship> itr = modelEntityRelationship
                .getAttributeRelationships().iterator();
        while (itr.hasNext()) {
            delete(itr.next());
        }
        persistenceManager.delete(modelEntityRelationship, null, null,
                tableName(ModelEntityRelationship.class));
    }

    @Override
    public void delete(ModelAttributeRelationship modelAttributeRelationship) {

        persistenceManager.delete(modelAttributeRelationship, null, null,
                tableName(ModelAttributeRelationship.class));
    }

    @Override
    public void refresh(ModelVersion modelVersion) {

        refresh((AbstractObject) modelVersion);
        refreshModelVersionRelations(modelVersion);
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
            attribute.setEntityId(modelEntity.getId());
            modelEntity.getModelAttributes().put(attribute.getName(), attribute);
        }
        modelEntity.getModelEntityRelationships().clear();
        Map<String, Object> entityRelationshipParams = new HashMap<String, Object>();
        entityRelationshipParams.put("sourceEntityId", modelEntity.getId());
        List<ModelEntityRelationship> entityRelationships = persistenceManager.find(
                ModelEntityRelationship.class, entityRelationshipParams, null, null,
                tableName(ModelEntityRelationship.class));
        for (ModelEntityRelationship entityRelationshipData : entityRelationships) {
            refresh(entityRelationshipData);
            modelEntity.getModelEntityRelationships().add(entityRelationshipData);
        }
    }

    @Override
    public void refresh(ModelAttribute modelAttribute) {
        refresh((AbstractObject) modelAttribute);
    }

    @Override
    public void refresh(ModelEntityRelationship modelEntityRelationship) {

        refresh((AbstractObject) modelEntityRelationship);
        Map<String, Object> entityRelationshipParams = new HashMap<String, Object>();
        entityRelationshipParams.put("entityRelationshipId", modelEntityRelationship.getId());
        modelEntityRelationship.getAttributeRelationships().clear();
        List<ModelAttributeRelationship> attributeRelationships = persistenceManager.find(
                ModelAttributeRelationship.class, entityRelationshipParams, null, null,
                tableName(ModelAttribute.class));
        for (ModelAttributeRelationship attributeRelationship : attributeRelationships) {
            refresh(attributeRelationship);
            modelEntityRelationship.getAttributeRelationships().add(attributeRelationship);
        }
    }

    @Override
    public void refresh(ModelAttributeRelationship modelAttributeRelationship) {

        refresh((AbstractObject) modelAttributeRelationship);
    }

    @Override
    public void save(ModelVersion modelVersion) {

        save((AbstractObject) modelVersion);

        Iterator<Entry<String, ModelEntity>> itr = modelVersion.getModelEntities().entrySet()
                .iterator();
        while (itr.hasNext()) {
            save(itr.next().getValue());
        }
    }

    @Override
    public void save(ModelEntity modelEntity) {

        save((AbstractObject) modelEntity);

        Iterator<Entry<String, ModelAttribute>> itra = modelEntity.getModelAttributes().entrySet()
                .iterator();
        while (itra.hasNext()) {
            save(itra.next().getValue());
        }
        Iterator<ModelEntityRelationship> itrr = modelEntity.getModelEntityRelationships()
                .iterator();
        while (itrr.hasNext()) {
            save(itrr.next());
        }
    }

    @Override
    public void save(ModelEntityRelationship modelEntityRelationship) {

        save((AbstractObject) modelEntityRelationship);
        for (ModelAttributeRelationship attributeRelationship : modelEntityRelationship
                .getAttributeRelationships()) {
            save(attributeRelationship);
        }
    }
}
