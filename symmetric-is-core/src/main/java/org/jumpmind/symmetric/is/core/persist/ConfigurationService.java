package org.jumpmind.symmetric.is.core.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.app.core.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.AbstractData;
import org.jumpmind.symmetric.is.core.config.data.ComponentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeLinkData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public class ConfigurationService implements IConfigurationService {

    protected IPersistenceManager persistenceManager;

    protected String tablePrefix;

    public ConfigurationService(IPersistenceManager persistenceManager, String tablePrefix) {
        this.persistenceManager = persistenceManager;
        this.tablePrefix = tablePrefix;
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
    public void deleteComponentFlowNode(ComponentFlowNode flowNode) {
        persistenceManager.delete(flowNode.getData(), null, null, tableName(flowNode.getData()
                .getClass()));
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
        List<ConnectionData> datas = persistenceManager.find(ConnectionData.class, params,
                null, null, tableName(ConnectionData.class));
        List<Connection> list = new ArrayList<Connection>();
        for (ConnectionData data : datas) {
            Connection obj = new Connection(folder, data);
            list.add(obj);
            
            // TODO settings
        }
        return list;
    }

    @Override
    public void deleteConnection(Connection connection) {
        persistenceManager.delete(connection.getData(), null, null, tableName(ConnectionData.class));
    }
    
    // TODO transactional
    @Override
    public void deleteComponentFlow(ComponentFlow flow) {
        List<ComponentFlowVersion> versions = flow.getComponentFlowVersions();
        for (ComponentFlowVersion componentFlowVersion : versions) {
            deleteComponentFlowVersion(componentFlowVersion);
        }

        persistenceManager.delete(flow.getData(), null, null, tableName(ComponentFlowData.class));

    }

    @Override
    public void deleteComponentFlowLink(ComponentFlowNodeLink link) {
        persistenceManager.delete(link.getData(), null, null, tableName(ComponentFlowNodeLinkData.class));
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
        // TODO
    }

    @Override
    public void refresh(ComponentFlowVersion componentFlowVersion) {
        persistenceManager.refresh(componentFlowVersion.getData(), null, null,
                tableName(componentFlowVersion.getData().getClass()));
        refreshComponentFlowVersionRelations(componentFlowVersion);
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
                ;
            }
        }

    }

    protected void delete(AbstractData data) {
        persistenceManager.delete(data, null, null, tableName(data.getClass()));
    }

    @Override
    public void save(AbstractObject<?> obj) {
        obj.getData().setLastModifyTime(new Date());
        persistenceManager.save(obj.getData(), null, null, tableName(obj.getData().getClass()));
    }

    // TODO transactional
    @Override
    public void save(ComponentFlowVersion flowVersion) {

        // TODO need to diff saved versus new and delete the deleted

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
            persistenceManager.save(link.getData(), null, null, tableName(link.getData().getClass()));
        }

    }

}
