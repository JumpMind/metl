package org.jumpmind.symmetric.is.core.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.config.Model;
import org.jumpmind.symmetric.is.core.config.ModelAttribute;
import org.jumpmind.symmetric.is.core.config.ModelAttributeRelationship;
import org.jumpmind.symmetric.is.core.config.ModelEntity;
import org.jumpmind.symmetric.is.core.config.ModelEntityRelationship;
import org.jumpmind.symmetric.is.core.config.ModelVersion;
import org.jumpmind.symmetric.is.core.config.data.AgentData;
import org.jumpmind.symmetric.is.core.config.data.AgentDeploymentData;
import org.jumpmind.symmetric.is.core.config.data.AgentSettingData;
import org.jumpmind.symmetric.is.core.config.data.ComponentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeLinkData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionSettingData;
import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.config.data.ConnectionSettingData;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.ModelAttributeData;
import org.jumpmind.symmetric.is.core.config.data.ModelAttributeRelationshipData;
import org.jumpmind.symmetric.is.core.config.data.ModelData;
import org.jumpmind.symmetric.is.core.config.data.ModelEntityData;
import org.jumpmind.symmetric.is.core.config.data.ModelEntityRelationshipData;
import org.jumpmind.symmetric.is.core.config.data.ModelVersionData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;
import org.jumpmind.symmetric.is.core.util.NameValue;

// TODO make methods transactional
abstract class AbstractConfigurationService extends AbstractService implements
		IConfigurationService {

	AbstractConfigurationService(IPersistenceManager persistenceManager,
			String tablePrefix) {
		super(persistenceManager, tablePrefix);
	}

	@Override
	public List<Folder> findFolders(FolderType type) {
		Map<String, Object> byType = new HashMap<String, Object>();
		byType.put("type", type.name());
		List<FolderData> folderDatas = find(FolderData.class, byType);

		List<Folder> allFolders = new ArrayList<Folder>();
		for (FolderData folderData : folderDatas) {
			allFolders.add(new Folder(folderData));
		}

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

	public List<ComponentFlow> findComponentFlowsInFolder(Folder folder) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("folderId", folder.getData().getId());
		List<ComponentFlowData> datas = find(ComponentFlowData.class, params);
		List<ComponentFlow> flows = new ArrayList<ComponentFlow>();
		for (ComponentFlowData componentFlowData : datas) {
			ComponentFlow flow = new ComponentFlow(folder, componentFlowData);
			flows.add(flow);

			Map<String, Object> versionParams = new HashMap<String, Object>();
			versionParams.put("componentFlowId", componentFlowData.getId());
			List<ComponentFlowVersionData> versionDatas = find(
					ComponentFlowVersionData.class, versionParams);
			for (ComponentFlowVersionData versionData : versionDatas) {
				ComponentFlowVersion version = new ComponentFlowVersion(flow,
						versionData);
				refreshComponentFlowVersionRelations(version);
				flow.getComponentFlowVersions().add(version);
			}

		}
		return flows;
	}

	public List<Connection> findConnectionsInFolder(Folder folder) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("folderId", folder.getData().getId());
		List<ConnectionData> datas = find(ConnectionData.class, params);
		return buildConnection(folder, datas);
	}

	@Override
	public List<Connection> findConnectionsByTypes(String... types) {
		List<Connection> list = new ArrayList<Connection>();
		if (types != null) {
			for (String type : types) {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("type", type);
				List<ConnectionData> datas = find(ConnectionData.class, params);
				list = buildConnection(null, datas);
			}
		}
		return list;
	}

	@Override
	public List<Agent> findAgentsInFolder(Folder folder) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("folderId", folder.getData().getId());
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
		List<AgentData> datas = persistenceManager.find(AgentData.class,
				params, null, null, tableName(AgentData.class));
		List<Agent> list = new ArrayList<Agent>();

		Map<String, Folder> folderMapById = new HashMap<String, Folder>();
		if (folder != null) {
			folderMapById.put(folder.getData().getId(), folder);
		} else {
			List<Folder> folders = findFolders(FolderType.RUNTIME);
			for (Folder folder2 : folders) {
				folderMapById.put(folder2.getData().getId(), folder2);
			}
		}

		for (AgentData data : datas) {
			Map<String, Object> settingParams = new HashMap<String, Object>();
			settingParams.put("agentId", data.getId());
			List<AgentSettingData> settings = persistenceManager.find(
					AgentSettingData.class, settingParams, null, null,
					tableName(AgentSettingData.class));
			Agent agent = new Agent(folderMapById.get(data.getFolderId()),
					data, settings.toArray(new SettingData[settings.size()]));
			list.add(agent);

			List<AgentDeploymentData> deploymentDatas = persistenceManager
					.find(AgentDeploymentData.class, settingParams, null, null,
							tableName(AgentDeploymentData.class));
			for (AgentDeploymentData agentDeploymentData : deploymentDatas) {
				ComponentFlowVersion componentFlowVersion = new ComponentFlowVersion(
						null,
						new ComponentFlowVersionData(agentDeploymentData
								.getComponentFlowVersionId()));
				refresh(componentFlowVersion);
				agent.getAgentDeployments().add(
						new AgentDeployment(componentFlowVersion,
								agentDeploymentData));
			}
		}
		return list;
	}

	@Override
	public List<AgentDeployment> findAgentDeploymentsFor(
			ComponentFlowVersion componentFlowVersion) {
		List<AgentDeploymentData> deploymentDatas = persistenceManager.find(
				AgentDeploymentData.class,
				new NameValue("componentFlowVersionId", componentFlowVersion
						.getId()), null, null,
				tableName(AgentDeploymentData.class));
		List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
				deploymentDatas.size());
		for (AgentDeploymentData agentDeploymentData : deploymentDatas) {
			deployments.add(new AgentDeployment(componentFlowVersion,
					agentDeploymentData));
		}
		return deployments;
	}

	@Override
	public Connection findConnection(String id) {
		Connection connection = null;
		ConnectionData data = findOne(ConnectionData.class, new NameValue("id",
				id));
		if (data != null) {
			connection = new Connection(data);
			refresh(connection);
		}
		return connection;
	}

	public ComponentVersion findComponentVersion(String id) {

		ComponentVersionData componentVersionData = new ComponentVersionData();
		componentVersionData.setId(id);
		persistenceManager.refresh(componentVersionData, null, null,
				tableName(ComponentVersionData.class));

		ComponentData componentData = new ComponentData();
		componentData.setId(componentVersionData.getComponentId());
		persistenceManager.refresh(componentData, null, null,
				tableName(ComponentData.class));
		Component component = new Component(componentData);

		ModelVersion inputModelVersion = findModelVersion(componentVersionData
				.getInputModelVersiondId());
		ModelVersion outputModelVersion = findModelVersion(componentVersionData
				.getOutputModelVersionId());

		List<ComponentVersionSettingData> settings = find(
				ComponentVersionSettingData.class, new NameValue(
						"componentVersionId", componentVersionData.getId()));

		return new ComponentVersion(component,
				findConnection(componentVersionData.getConnectionId()),
				inputModelVersion, outputModelVersion, componentVersionData,
				settings.toArray(new SettingData[settings.size()]));
	}

	public ModelVersion findModelVersion(String id) {

		return null;
	}

	protected List<Connection> buildConnection(Folder folder,
			List<ConnectionData> datas) {
		return buildConnection(folder,
				datas.toArray(new ConnectionData[datas.size()]));
	}

	protected List<Connection> buildConnection(Folder folder,
			ConnectionData... datas) {
		List<Connection> list = new ArrayList<Connection>();
		for (ConnectionData data : datas) {
			if (folder == null) {
				Map<String, Object> folderParams = new HashMap<String, Object>();
				folderParams.put("id", data.getFolderId());
				folder = new Folder(findOne(FolderData.class, folderParams));
			}

			Map<String, Object> settingParams = new HashMap<String, Object>();
			settingParams.put("connectionId", data.getId());
			List<ConnectionSettingData> settings = find(
					ConnectionSettingData.class, settingParams);
			list.add(new Connection(folder, data, settings
					.toArray(new SettingData[settings.size()])));
		}
		return list;
	}

	@Override
	public void delete(AgentDeployment agentDeployment) {
		delete(agentDeployment.getData());
	}

	@Override
	public void delete(ComponentFlowVersion componentFlowVersion,
			ComponentFlowNode flowNode) {
		List<ComponentFlowNodeLink> links = componentFlowVersion
				.removeComponentFlowNodeLinks(flowNode.getData().getId());
		for (ComponentFlowNodeLink link : links) {
			delete(link);
		}

		componentFlowVersion.removeComponentFlowNode(flowNode);
		delete(flowNode.getData());
	}

	@Override
	public void deleteFolder(String folderId) {
		Map<String, Object> byType = new HashMap<String, Object>();
		byType.put("parentFolderId", folderId);
		List<FolderData> folderDatas = find(FolderData.class, byType);
		for (FolderData folderData : folderDatas) {
			deleteFolder(folderData.getId());
		}
		persistenceManager.delete(new FolderData(folderId), null, null,
				tableName(FolderData.class));
	}

	@Override
	public void delete(Connection connection) {
		List<SettingData> settings = connection.getSettings();
		for (SettingData settingData : settings) {
			delete(settingData);
		}
		delete(connection.getData());
	}

	@Override
	public void delete(Agent agent) {
		List<SettingData> settings = agent.getSettings();
		for (SettingData settingData : settings) {
			delete(settingData);
		}
		delete(agent.getData());
	}

	@Override
	public void delete(ComponentFlow flow) {
		List<ComponentFlowVersion> versions = flow.getComponentFlowVersions();
		for (ComponentFlowVersion componentFlowVersion : versions) {
			deleteComponentFlowVersion(componentFlowVersion);
		}

		persistenceManager.delete(flow.getData(), null, null,
				tableName(ComponentFlowData.class));

	}

	@Override
	public void delete(ComponentFlowNodeLink link) {
		persistenceManager.delete(link.getData(), null, null,
				tableName(ComponentFlowNodeLinkData.class));
	}

	@Override
	public void deleteComponentFlowVersion(ComponentFlowVersion flowVersion) {

		List<ComponentFlowNodeLink> links = flowVersion
				.getComponentFlowNodeLinks();
		for (ComponentFlowNodeLink link : links) {
			delete(link.getData());
		}
		List<ComponentFlowNode> nodes = flowVersion.getComponentFlowNodes();
		for (ComponentFlowNode node : nodes) {
			delete(node.getData());

			ComponentVersion componentVersion = node.getComponentVersion();
			Component component = componentVersion.getComponent();
			if (!component.getData().isShared()) {
				/*
				 * I do not think there will ever be more than one version of a
				 * non shared component
				 */
				delete(componentVersion.getData());
				delete(component.getData());
			}
		}
		delete(flowVersion.getData());
	}

	@Override
	public void refresh(Connection connection) {
		refresh((AbstractObject<?>) connection);

		ConnectionData data = connection.getData();
		Map<String, Object> folderParams = new HashMap<String, Object>();
		folderParams.put("id", data.getFolderId());
		connection
				.setFolder(new Folder(findOne(FolderData.class, folderParams)));

		Map<String, Object> settingParams = new HashMap<String, Object>();
		settingParams.put("connectionId", data.getId());
		List<? extends SettingData> settings = find(
				ConnectionSettingData.class, settingParams);
		connection.setSettings(settings);
	}

	@Override
	public void refresh(Model model) {
		refresh((AbstractObject<?>) model);

		ModelData data = model.getData();
		Map<String, Object> folderParams = new HashMap<String, Object>();
		folderParams.put("id", data.getFolderId());
		model.setFolder(new Folder(findOne(FolderData.class, folderParams)));
	}

	@Override
	public void refresh(Agent agent) {
		refresh((AbstractObject<?>) agent);

		// TODO refresh settings
	}

	@Override
	public void refresh(ComponentFlowVersion componentFlowVersion) {
		refresh((AbstractObject<?>) componentFlowVersion);
		refreshComponentFlowVersionRelations(componentFlowVersion);
	}

	private void refreshComponentFlowVersionRelations(
			ComponentFlowVersion componentFlowVersion) {
		ComponentFlow flow = componentFlowVersion.getComponentFlow();
		if (flow == null) {
			flow = new ComponentFlow(null, new ComponentFlowData(
					componentFlowVersion.getData().getComponentFlowId()));
			componentFlowVersion.setComponentFlow(flow);
		}
		refresh(flow);

		componentFlowVersion.getComponentFlowNodes().clear();
		componentFlowVersion.getComponentFlowNodeLinks().clear();
		Map<String, Object> versionParams = new HashMap<String, Object>();
		versionParams.put("componentFlowVersionId", componentFlowVersion
				.getData().getId());
		List<ComponentFlowNodeData> datas = persistenceManager.find(
				ComponentFlowNodeData.class, versionParams, null, null,
				tableName(ComponentFlowNodeData.class));
		for (ComponentFlowNodeData data : datas) {
			ComponentFlowNode node = new ComponentFlowNode(
					findComponentVersion(data.getComponentVersionId()), data);
			componentFlowVersion.getComponentFlowNodes().add(node);

			Map<String, Object> linkParams = new HashMap<String, Object>();
			linkParams.put("sourceNodeId", data.getId());

			List<ComponentFlowNodeLinkData> dataLinks = persistenceManager
					.find(ComponentFlowNodeLinkData.class, linkParams, null,
							null, tableName(ComponentFlowNodeLinkData.class));
			for (ComponentFlowNodeLinkData dataLink : dataLinks) {
				componentFlowVersion.getComponentFlowNodeLinks().add(
						new ComponentFlowNodeLink(dataLink));
			}
		}

	}

	@Override
	public void save(Connection connection) {
		save((AbstractObject<?>) connection);
		List<SettingData> settings = connection.getSettings();
		for (SettingData settingData : settings) {
			save(settingData);
		}
	}

	@Override
	public void save(ComponentFlowNode componentFlowNode) {
		ComponentVersion version = componentFlowNode.getComponentVersion();
		Component component = version.getComponent();
		if (!component.getData().isShared()) {
			save(component);
			save(version);
		}
		save((AbstractObject<?>) componentFlowNode);
	}

	@Override
	public void save(ComponentFlowVersion flowVersion) {

		save((AbstractObject<?>) flowVersion);

		List<ComponentFlowNode> componentFlowNodes = flowVersion
				.getComponentFlowNodes();
		for (ComponentFlowNode componentFlowNode : componentFlowNodes) {
			save(componentFlowNode);
		}

		List<ComponentFlowNodeLink> links = flowVersion
				.getComponentFlowNodeLinks();
		for (ComponentFlowNodeLink link : links) {
			link.getData().setLastModifyTime(new Date());
			persistenceManager.save(link.getData(), null, null, tableName(link
					.getData().getClass()));
		}

	}

	@Override
	public void delete(Model model) {

		List<ModelVersion> versions = model.getModelVersions();
		for (ModelVersion modelVersion : versions) {
			delete(modelVersion);
		}

		persistenceManager.delete(model.getData(), null, null,
				tableName(ModelData.class));

	}

	@Override
	public void delete(ModelVersion modelVersion) {

		List<ModelEntity> entities = modelVersion.getModelEntities();
		for (ModelEntity entity : entities) {
			delete(entity);
		}

		persistenceManager.delete(modelVersion.getData(), null, null,
				tableName(ModelVersionData.class));
	}

	@Override
	public void delete(ModelEntity modelEntity) {

		List<ModelEntityRelationship> entityRelationships = modelEntity
				.getModelEntityRelationships();
		for (ModelEntityRelationship entityRelationship : entityRelationships) {
			delete(entityRelationship);
		}

		List<ModelAttribute> attributes = modelEntity.getModelAttributes();
		for (ModelAttribute attribute : attributes) {
			delete(attribute);
		}

		persistenceManager.delete(modelEntity.getData(), null, null,
				tableName(ModelEntity.class));
	}

	@Override
	public void delete(ModelAttribute modelAttribute) {

		persistenceManager.delete(modelAttribute.getData(), null, null,
				tableName(ModelAttribute.class));
	}

	@Override
	public void delete(ModelEntityRelationship modelEntityRelationship) {

		List<ModelAttributeRelationship> attributeRelationships = modelEntityRelationship
				.getAttributeRelationships();
		for (ModelAttributeRelationship attributeRelationship : attributeRelationships) {
			delete(attributeRelationship);
		}

		persistenceManager.delete(modelEntityRelationship.getData(), null,
				null, tableName(ModelEntityRelationship.class));
	}

	@Override
	public void delete(ModelAttributeRelationship modelAttributeRelationship) {

		persistenceManager.delete(modelAttributeRelationship.getData(), null,
				null, tableName(ModelAttributeRelationship.class));
	}

	@Override
	public void refresh(ModelVersion modelVersion) {

		refresh((AbstractObject<?>) modelVersion);
		Model model = modelVersion.getModel();
		if (model == null) {
			model = new Model(new ModelData(modelVersion.getData().getId()));
			modelVersion.setModel(model);
		}
		refresh(model);

		modelVersion.getModelEntities().clear();
		Map<String, Object> versionParams = new HashMap<String, Object>();
		versionParams.put("modelVersionId", modelVersion.getData().getId());
		List<ModelEntityData> entityDatas = persistenceManager.find(
				ModelEntityData.class, versionParams, null, null,
				tableName(ModelEntityData.class));
		for (ModelEntityData entityData : entityDatas) {
			ModelEntity modelEntity = new ModelEntity(modelVersion, entityData);
			refresh(modelEntity);
			modelVersion.getModelEntities().add(modelEntity);
		}
	}

	@Override
	public void refresh(ModelEntity modelEntity) {

		refresh((AbstractObject<?>) modelEntity);
		Map<String, Object> entityParams = new HashMap<String, Object>();
		entityParams.put("entityId", modelEntity.getData().getId());
		modelEntity.getModelAttributes().clear();
		List<ModelAttributeData> attributeDatas = persistenceManager.find(
				ModelAttributeData.class, entityParams, null, null,
				tableName(ModelAttributeData.class));
		for (ModelAttributeData attributeData : attributeDatas) {
			ModelAttribute modelAttribute = new ModelAttribute(modelEntity,
					null, attributeData);
			refresh(modelAttribute);
			modelEntity.getModelAttributes().add(modelAttribute);
		}
		modelEntity.getModelEntityRelationships().clear();
		List<ModelEntityRelationshipData> entityRelationshipDatas = persistenceManager
				.find(ModelEntityRelationshipData.class, entityParams, null,
						null, tableName(ModelEntityRelationshipData.class));
		for (ModelEntityRelationshipData entityRelationshipData : entityRelationshipDatas) {
			ModelEntityRelationship modelEntityRelationship = new ModelEntityRelationship(
					entityRelationshipData);
			refresh(modelEntityRelationship);
			modelEntity.getModelEntityRelationships().add(
					modelEntityRelationship);
		}
	}

	@Override
	public void refresh(ModelAttribute modelAttribute) {
		refresh((AbstractObject<?>) modelAttribute);
	}

	@Override
	public void refresh(ModelEntityRelationship modelEntityRelationship) {

		refresh((AbstractObject<?>) modelEntityRelationship);
		Map<String, Object> entityRelationshipParams = new HashMap<String, Object>();
		entityRelationshipParams.put("entityRelationshipId",
				modelEntityRelationship.getData().getId());
		modelEntityRelationship.getAttributeRelationships().clear();
		List<ModelAttributeRelationshipData> attributeRelationshipDatas = persistenceManager
				.find(ModelAttributeRelationshipData.class,
						entityRelationshipParams, null, null,
						tableName(ModelAttributeData.class));
		for (ModelAttributeRelationshipData attributeRelationshipData : attributeRelationshipDatas) {
			ModelAttributeRelationship modelAttributeRelationship = new ModelAttributeRelationship(
					attributeRelationshipData);
			refresh(modelAttributeRelationship);
			modelEntityRelationship.getAttributeRelationships().add(
					modelAttributeRelationship);
		}
	}

	@Override
	public void refresh(ModelAttributeRelationship modelAttributeRelationship) {

		refresh((AbstractObject<?>) modelAttributeRelationship);
	}

	@Override
	public void save(ModelVersion modelVersion) {

		save((AbstractObject<?>) modelVersion);
		for (ModelEntity entity : modelVersion.getModelEntities()) {
			save(entity);
		}
	}

	@Override
	public void save(ModelEntity modelEntity) {

		save((AbstractObject<?>) modelEntity);
		for (ModelAttribute attribute : modelEntity.getModelAttributes()) {
			save(attribute);
		}
		for (ModelEntityRelationship entityRelationship : modelEntity
				.getModelEntityRelationships()) {
			save(entityRelationship);
		}
	}

	@Override
	public void save(ModelEntityRelationship modelEntityRelationship) {

		save((AbstractObject<?>) modelEntityRelationship);
		for (ModelAttributeRelationship attributeRelationship : modelEntityRelationship
				.getAttributeRelationships()) {
			save(attributeRelationship);
		}
	}
}
