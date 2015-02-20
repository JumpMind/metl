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
import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.AgentSetting;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersionSetting;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.ConnectionSetting;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.config.Format;
import org.jumpmind.symmetric.is.core.config.FormatVersion;
import org.jumpmind.symmetric.is.core.config.Model;
import org.jumpmind.symmetric.is.core.config.ModelAttribute;
import org.jumpmind.symmetric.is.core.config.ModelAttributeRelationship;
import org.jumpmind.symmetric.is.core.config.ModelEntity;
import org.jumpmind.symmetric.is.core.config.ModelEntityRelationship;
import org.jumpmind.symmetric.is.core.config.ModelVersion;
import org.jumpmind.symmetric.is.core.config.Setting;
import org.jumpmind.symmetric.is.core.util.NameValue;

// TODO make methods transactional
abstract class AbstractConfigurationService extends AbstractService implements
        IConfigurationService {

    AbstractConfigurationService(IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
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
    public List<ComponentFlow> findComponentFlowsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("folderId", folder.getId());
        List<ComponentFlow> flows = find(ComponentFlow.class, params);
        for (ComponentFlow flow : flows) {
            flow.setFolder(folder);
            Map<String, Object> versionParams = new HashMap<String, Object>();
            versionParams.put("componentFlowId", flow.getId());
            List<ComponentFlowVersion> versionDatas = find(ComponentFlowVersion.class,
                    versionParams);
            for (ComponentFlowVersion version : versionDatas) {
                version.setComponentFlow(flow);
                refreshComponentFlowVersionRelations(version);
                flow.getComponentFlowVersions().add(version);
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
            models.add(model);
        }

        return models;
    }

    @Override
    public List<Format> findFormatsInFolder(Folder folder) {
    	Map<String, Object> params = new HashMap<String, Object>();
        params.put("folderId", folder.getId());
        List<Format> formats = find(Format.class, params);
        for (Format format : formats) {
        	format.setFolder(folder);        	
            Map<String, Object> versionParams = new HashMap<String, Object>();
            versionParams.put("formatId", format.getId());
            List<FormatVersion> formatVersions = find(FormatVersion.class, versionParams);
            for (FormatVersion formatVersion : formatVersions) {
            	formatVersion.setFormat(format);
            	refreshFormatVersionRelations(formatVersion);
                format.getFormatVersions().add(formatVersion);
            }
            formats.add(format);
        }

        return formats;
    }

    protected void refreshFormatVersionRelations(FormatVersion formatVersion) {
    	
    	//TODO:

    }
    
    @Override
    public List<Connection> findConnectionsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("folderId", folder.getId());
        List<Connection> datas = find(Connection.class, params);
        return buildConnection(folder, datas);
    }

    @Override
    public List<Connection> findConnectionsByTypes(String... types) {
        List<Connection> list = new ArrayList<Connection>();
        if (types != null) {
            for (String type : types) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("type", type);
                List<Connection> datas = find(Connection.class, params);
                list = buildConnection(null, datas);
            }
        }
        return list;
    }

    @Override
    public List<Agent> findAgentsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("folderId", folder.getId());
        return findAgents(params, folder);
    }

    @Override
    public List<Agent> findAgentsForHost(String hostName) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("host", hostName);
        return findAgents(params);
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
                ComponentFlowVersion componentFlowVersion = new ComponentFlowVersion(null);
                componentFlowVersion.setId(agentDeployment.getComponentFlowVersionId());
                refresh(componentFlowVersion);
                agentDeployment.setComponentFlowVersion(componentFlowVersion);
                agent.getAgentDeployments().add(agentDeployment);
            }
        }
        return list;
    }

    @Override
    public List<AgentDeployment> findAgentDeploymentsFor(ComponentFlowVersion componentFlowVersion) {
        List<AgentDeployment> deployments = persistenceManager.find(AgentDeployment.class,
                new NameValue("componentFlowVersionId", componentFlowVersion.getId()), null, null,
                tableName(AgentDeployment.class));
        for (AgentDeployment deployment : deployments) {
            deployment.setComponentFlowVersion(componentFlowVersion);
        }
        return deployments;
    }

    @Override
    public Connection findConnection(String id) {
        Connection connection = findOne(Connection.class, new NameValue("id", id));
        if (connection != null) {
            refresh(connection);
        }
        return connection;
    }

    protected ComponentVersion findComponentVersion(String id) {
        ComponentVersion componentVersion = new ComponentVersion();
        componentVersion.setId(id);
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

        componentVersion.setConnection(findConnection(componentVersion.getConnectionId()));

        return componentVersion;

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

    protected List<Connection> buildConnection(Folder folder, List<Connection> datas) {
        return buildConnection(folder, datas.toArray(new Connection[datas.size()]));
    }

    protected List<Connection> buildConnection(Folder folder, Connection... connections) {
        List<Connection> list = new ArrayList<Connection>();
        for (Connection connection : connections) {
            if (folder == null) {
                Map<String, Object> folderParams = new HashMap<String, Object>();
                folderParams.put("id", connection.getFolderId());
                folder = findOne(Folder.class, folderParams);
            }

            Map<String, Object> settingParams = new HashMap<String, Object>();
            settingParams.put("connectionId", connection.getId());
            List<ConnectionSetting> settings = find(ConnectionSetting.class, settingParams);
            connection.setSettings(settings);
            connection.setFolder(folder);
            list.add(connection);
        }
        return list;
    }

    @Override
    public void delete(AgentDeployment agentDeployment) {
        delete(agentDeployment);
    }

    @Override
    public void delete(ComponentFlowVersion componentFlowVersion, ComponentFlowNode flowNode) {
        List<ComponentFlowNodeLink> links = componentFlowVersion
                .removeComponentFlowNodeLinks(flowNode.getId());
        for (ComponentFlowNodeLink link : links) {
            delete(link);
        }

        componentFlowVersion.removeComponentFlowNode(flowNode);
        delete(flowNode);
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
    public void delete(Connection connection) {
        List<Setting> settings = connection.getSettings();
        for (Setting settingData : settings) {
            delete(settingData);
        }
        delete(connection);
    }

    @Override
    public void delete(Agent agent) {
        List<Setting> settings = agent.getSettings();
        for (Setting settingData : settings) {
            delete(settingData);
        }
        delete(agent);
    }

    @Override
    public void delete(ComponentFlow flow) {
        List<ComponentFlowVersion> versions = flow.getComponentFlowVersions();
        for (ComponentFlowVersion componentFlowVersion : versions) {
            deleteComponentFlowVersion(componentFlowVersion);
        }

        persistenceManager.delete(flow, null, null, tableName(ComponentFlow.class));

    }

    @Override
    public void delete(ComponentFlowNodeLink link) {
        persistenceManager.delete(link, null, null, tableName(ComponentFlowNodeLink.class));
    }

    @Override
    public void deleteComponentFlowVersion(ComponentFlowVersion flowVersion) {

        List<ComponentFlowNodeLink> links = flowVersion.getComponentFlowNodeLinks();
        for (ComponentFlowNodeLink link : links) {
            delete(link);
        }
        List<ComponentFlowNode> nodes = flowVersion.getComponentFlowNodes();
        for (ComponentFlowNode node : nodes) {
            delete(node);

            ComponentVersion componentVersion = node.getComponentVersion();
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
    public void refresh(Connection connection) {
        refresh((AbstractObject) connection);

        Map<String, Object> folderParams = new HashMap<String, Object>();
        folderParams.put("id", connection.getFolderId());
        connection.setFolder(findOne(Folder.class, folderParams));

        Map<String, Object> settingParams = new HashMap<String, Object>();
        settingParams.put("connectionId", connection.getId());
        List<? extends Setting> settings = find(ConnectionSetting.class, settingParams);
        connection.setSettings(settings);
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
    public void refresh(ComponentFlowVersion componentFlowVersion) {
        refresh((AbstractObject) componentFlowVersion);
        refreshComponentFlowVersionRelations(componentFlowVersion);
    }

    private void refreshComponentFlowVersionRelations(ComponentFlowVersion componentFlowVersion) {
        ComponentFlow flow = componentFlowVersion.getComponentFlow();
        if (flow == null) {
            flow = new ComponentFlow(componentFlowVersion.getId());
            componentFlowVersion.setComponentFlow(flow);
        }
        refresh(flow);

        componentFlowVersion.getComponentFlowNodes().clear();
        componentFlowVersion.getComponentFlowNodeLinks().clear();
        Map<String, Object> versionParams = new HashMap<String, Object>();
        versionParams.put("componentFlowVersionId", componentFlowVersion.getId());
        List<ComponentFlowNode> nodes = persistenceManager.find(ComponentFlowNode.class,
                versionParams, null, null, tableName(ComponentFlowNode.class));
        for (ComponentFlowNode node : nodes) {
            node.setComponentVersion(findComponentVersion(node.getComponentVersionId()));
            componentFlowVersion.getComponentFlowNodes().add(node);

            Map<String, Object> linkParams = new HashMap<String, Object>();
            linkParams.put("sourceNodeId", node.getId());

            List<ComponentFlowNodeLink> dataLinks = persistenceManager.find(
                    ComponentFlowNodeLink.class, linkParams, null, null,
                    tableName(ComponentFlowNodeLink.class));
            for (ComponentFlowNodeLink dataLink : dataLinks) {
                componentFlowVersion.getComponentFlowNodeLinks().add(dataLink);
            }
        }

    }

    @Override
    public void save(Connection connection) {
        save((AbstractObject) connection);
        List<Setting> settings = connection.getSettings();
        for (Setting settingData : settings) {
            save(settingData);
        }
    }

    @Override
    public void save(ComponentFlowNode componentFlowNode) {
        ComponentVersion version = componentFlowNode.getComponentVersion();
        Component component = version.getComponent();
        if (!component.isShared()) {
            save(component);
            save(version);
        }
        save((AbstractObject) componentFlowNode);
    }

    @Override
    public void save(ComponentFlowVersion flowVersion) {

        save((AbstractObject) flowVersion);

        List<ComponentFlowNode> componentFlowNodes = flowVersion.getComponentFlowNodes();
        for (ComponentFlowNode componentFlowNode : componentFlowNodes) {
            save(componentFlowNode);
        }

        List<ComponentFlowNodeLink> links = flowVersion.getComponentFlowNodeLinks();
        for (ComponentFlowNodeLink link : links) {
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
            modelEntity.getModelAttributes().put(attribute.getName(), attribute);
        }
        modelEntity.getModelEntityRelationships().clear();
        List<ModelEntityRelationship> entityRelationships = persistenceManager.find(
                ModelEntityRelationship.class, entityParams, null, null,
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
