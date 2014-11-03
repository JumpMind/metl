package org.jumpmind.symmetric.is.core.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.symmetric.app.core.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersionSummary;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;
import org.jumpmind.symmetric.is.core.config.data.AgentData;
import org.jumpmind.symmetric.is.core.config.data.AgentDeploymentData;
import org.jumpmind.symmetric.is.core.config.data.AgentSettingData;
import org.jumpmind.symmetric.is.core.config.data.ComponentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeLinkData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.config.data.ConnectionSettingData;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class ConfigurationService implements IConfigurationService {

    protected IPersistenceManager persistenceManager;

    protected String tablePrefix;

    protected IDatabasePlatform databasePlatform;

    public ConfigurationService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        this.persistenceManager = persistenceManager;
        this.tablePrefix = tablePrefix;
        this.databasePlatform = databasePlatform;
    }

    protected String tableName(Class<?> clazz) {
        StringBuilder name = new StringBuilder(tablePrefix);
        String[] tokens = StringUtils.splitByCharacterTypeCamelCase(clazz.getSimpleName()
                .substring(0, clazz.getSimpleName().indexOf("Data")));
        for (String string : tokens) {
            name.append("_");
            name.append(string);
        }
        return name.toString();
    }
    
    @Override
    public void delete(AgentDeployment agentDeployment) {
        delete(agentDeployment.getData());
    }

    @Override
    public void delete(ComponentFlowNode flowNode) {
        delete(flowNode.getData());
    }

    // TODO transactional
    @Override
    public void deleteFolder(String folderId) {
        Map<String, Object> byType = new HashMap<String, Object>();
        byType.put("parentFolderId", folderId);
        List<FolderData> folderDatas = persistenceManager.find(FolderData.class, byType, null,
                null, tableName(FolderData.class));
        for (FolderData folderData : folderDatas) {
            deleteFolder(folderData.getId());
        }
        persistenceManager
                .delete(new FolderData(folderId), null, null, tableName(FolderData.class));
    }

    @Override
    public List<Folder> findFolders(FolderType type) {
        Map<String, Object> byType = new HashMap<String, Object>();
        byType.put("type", type.name());
        List<FolderData> folderDatas = persistenceManager.find(FolderData.class, byType, null,
                null, tableName(FolderData.class));

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
        List<ComponentFlowData> datas = persistenceManager.find(ComponentFlowData.class, params,
                null, null, tableName(ComponentFlowData.class));
        List<ComponentFlow> flows = new ArrayList<ComponentFlow>();
        for (ComponentFlowData componentFlowData : datas) {
            ComponentFlow flow = new ComponentFlow(folder, componentFlowData);
            flows.add(flow);

            Map<String, Object> versionParams = new HashMap<String, Object>();
            versionParams.put("componentFlowId", componentFlowData.getId());
            List<ComponentFlowVersionData> versionDatas = persistenceManager.find(
                    ComponentFlowVersionData.class, versionParams, null, null,
                    tableName(ComponentFlowVersionData.class));
            for (ComponentFlowVersionData versionData : versionDatas) {
                ComponentFlowVersion version = new ComponentFlowVersion(flow, versionData);
                refreshComponentFlowVersionRelations(version);
                flow.getComponentFlowVersions().add(version);
            }

        }
        return flows;
    }

    public List<Connection> findConnectionsInFolder(Folder folder) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("folderId", folder.getData().getId());
        List<ConnectionData> datas = persistenceManager.find(ConnectionData.class, params, null,
                null, tableName(ConnectionData.class));
        List<Connection> list = new ArrayList<Connection>();
        for (ConnectionData data : datas) {
            Map<String, Object> settingParams = new HashMap<String, Object>();
            settingParams.put("connectionId", data.getId());
            List<ConnectionSettingData> settings = persistenceManager.find(
                    ConnectionSettingData.class, settingParams, null, null,
                    tableName(ConnectionSettingData.class));
            list.add(new Connection(folder, data,
                    settings.toArray(new SettingData[settings.size()])));
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
        List<AgentData> datas = persistenceManager.find(AgentData.class, params, null, null,
                tableName(AgentData.class));
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
            List<AgentSettingData> settings = persistenceManager.find(AgentSettingData.class,
                    settingParams, null, null, tableName(AgentSettingData.class));
            Agent agent = new Agent(folderMapById.get(data.getFolderId()), data, settings
                    .toArray(new SettingData[settings.size()]));
            list.add(agent);
            
            List<AgentDeploymentData> deploymentDatas = persistenceManager.find(AgentDeploymentData.class, settingParams, null, null, tableName(AgentDeploymentData.class));
            for (AgentDeploymentData agentDeploymentData : deploymentDatas) {
                ComponentFlowVersion componentFlowVersion = new ComponentFlowVersion(null,
                        new ComponentFlowVersionData(agentDeploymentData.getComponentFlowVersionId()));
                refresh(componentFlowVersion);
                agent.getAgentDeployments().add(new AgentDeployment(componentFlowVersion, agentDeploymentData));
            }
        }
        return list;

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

    // TODO transactional
    @Override
    public void delete(ComponentFlow flow) {
        List<ComponentFlowVersion> versions = flow.getComponentFlowVersions();
        for (ComponentFlowVersion componentFlowVersion : versions) {
            deleteComponentFlowVersion(componentFlowVersion);
        }

        persistenceManager.delete(flow.getData(), null, null, tableName(ComponentFlowData.class));

    }

    @Override
    public void delete(ComponentFlowNodeLink link) {
        persistenceManager.delete(link.getData(), null, null,
                tableName(ComponentFlowNodeLinkData.class));
    }

    @Override
    public void deleteComponentFlowVersion(ComponentFlowVersion flowVersion) {

        List<ComponentFlowNodeLink> links = flowVersion.getComponentFlowNodeLinks();
        for (ComponentFlowNodeLink link : links) {
            persistenceManager.delete(link.getData(), null, null, tableName(link.getData()
                    .getClass()));
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
        persistenceManager.delete(flowVersion.getData(), null, null,
                tableName(ComponentFlowVersionData.class));
    }

    @Override
    public void refresh(Connection connection) {
        refresh((AbstractObject<?>) connection);

        // TODO refresh settings
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

    protected void refresh(AbstractObject<?> object) {
        persistenceManager.refresh(object.getData(), null, null, tableName(object.getData()
                .getClass()));
    }

    public ComponentVersion findComponentVersion(String id) {
        ComponentVersionData componentVersionData = new ComponentVersionData();
        componentVersionData.setId(id);
        persistenceManager.refresh(componentVersionData, null, null,
                tableName(ComponentVersionData.class));
        ComponentData componentData = new ComponentData();
        componentData.setId(componentVersionData.getComponentId());
        persistenceManager.refresh(componentData, null, null, tableName(ComponentData.class));
        Component component = new Component(componentData);
        // TODO read connection, models and settings
        ComponentVersion version = new ComponentVersion(component, null, componentVersionData);
        return version;
    }

    private void refreshComponentFlowVersionRelations(ComponentFlowVersion componentFlowVersion) {
        ComponentFlow flow = componentFlowVersion.getComponentFlow();
        if (flow == null) {
            flow = new ComponentFlow(null, new ComponentFlowData(componentFlowVersion.getData().getComponentFlowId()));
            componentFlowVersion.setComponentFlow(flow);
        }
        refresh(flow);
        
        componentFlowVersion.getComponentFlowNodes().clear();
        componentFlowVersion.getComponentFlowNodeLinks().clear();
        Map<String, Object> versionParams = new HashMap<String, Object>();
        versionParams.put("componentFlowVersionId", componentFlowVersion.getData().getId());
        List<ComponentFlowNodeData> datas = persistenceManager.find(ComponentFlowNodeData.class,
                versionParams, null, null, tableName(ComponentFlowNodeData.class));
        for (ComponentFlowNodeData data : datas) {
            ComponentFlowNode node = new ComponentFlowNode(
                    findComponentVersion(data.getComponentVersionId()), data);
            componentFlowVersion.getComponentFlowNodes().add(node);

            Map<String, Object> linkParams = new HashMap<String, Object>();
            linkParams.put("sourceNodeId", data.getId());

            List<ComponentFlowNodeLinkData> dataLinks = persistenceManager.find(
                    ComponentFlowNodeLinkData.class, linkParams, null, null,
                    tableName(ComponentFlowNodeLinkData.class));
            for (ComponentFlowNodeLinkData dataLink : dataLinks) {
                componentFlowVersion.getComponentFlowNodeLinks().add(
                        new ComponentFlowNodeLink(dataLink));
            }
        }

    }

    protected void delete(AbstractData data) {
        persistenceManager.delete(data, null, null, tableName(data.getClass()));
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
    public void save(AbstractObject<?> obj) {
        save(obj.getData());
    }

    protected void save(AbstractData data) {
        data.setLastModifyTime(new Date());
        persistenceManager.save(data, null, null, tableName(data.getClass()));
    }

    // TODO transactional
    @Override
    public void save(ComponentFlowVersion flowVersion) {

        save((AbstractObject<?>) flowVersion);

        List<ComponentFlowNode> componentFlowNodes = flowVersion.getComponentFlowNodes();
        for (ComponentFlowNode componentFlowNode : componentFlowNodes) {
            ComponentVersion version = componentFlowNode.getComponentVersion();
            Component component = version.getComponent();
            if (!component.getData().isShared()) {
                save(component);
                save(version);
            }
            save(componentFlowNode);
        }

        List<ComponentFlowNodeLink> links = flowVersion.getComponentFlowNodeLinks();
        for (ComponentFlowNodeLink link : links) {
            link.getData().setLastModifyTime(new Date());
            persistenceManager.save(link.getData(), null, null,
                    tableName(link.getData().getClass()));
        }

    }

    // TODO subclass off of ConfigurationService with ConfigurationSqlService and put this there.
    // put sql in a json or xml file
    @Override
    public List<ComponentFlowVersionSummary> findUndeployedComponentFlowVersionSummary(
            String agentId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template
                .query(String
                        .format("select cf.name as name, cfv.version_name as version_name, f.name as folder_name, cfv.id as id from "
                                + "%1$s_component_flow_version cfv inner join "
                                + "%1$s_component_flow cf on cf.id=cfv.component_flow_id inner join "
                                + "%1$s_folder f on f.id=cf.folder_id "
                                + "where cf.id not in (select component_flow_id from %1$s_component_flow_version where id in "
                                + "(select component_flow_version_id from %1$s_agent_deployment where agent_id=?))",
                                tablePrefix), new ISqlRowMapper<ComponentFlowVersionSummary>() {
                    @Override
                    public ComponentFlowVersionSummary mapRow(Row row) {
                        ComponentFlowVersionSummary summary = new ComponentFlowVersionSummary();
                        summary.setName(row.getString("name"));
                        summary.setId(row.getString("id"));
                        summary.setFolderName(row.getString("folder_name"));
                        summary.setVersionName(row.getString("version_name"));
                        return summary;
                    }
                }, agentId);
    }

}
