package org.jumpmind.symmetric.is.core.persist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.app.core.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentGraph;
import org.jumpmind.symmetric.is.core.config.ComponentGraphVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.StructuredModel;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public class ConfigurationService {

    protected IPersistenceManager persistenceManager;

    protected String tablePrefix;

    public ConfigurationService(IPersistenceManager persistenceManager, String tablePrefix) {
        this.persistenceManager = persistenceManager;
        this.tablePrefix = tablePrefix;
    }

    protected String tableName(Class<?> clazz) {
        return tablePrefix + "_"
                + clazz.getSimpleName().substring(0, clazz.getSimpleName().indexOf("Data"));
    }

    public void save(ComponentGraphVersion graph) {
    }

    public void save(ComponentVersion component) {
    }

    public void save(Component component) {
    }

    public void save(ComponentGraph component) {
    }

    public void save(Folder folder) {
        persistenceManager.save(folder.getData(), null, null, tableName(FolderData.class));
    }

    // TODO transactional
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

    public List<Component> findComponents(Folder folder) {
        return null;
    }

    public List<ComponentGraph> findComponentGraphs(Folder folder) {
        return null;
    }

    public List<StructuredModel> findStructuredModel(Folder folder) {
        return null;
    }

    public List<Connection> findConnections(Folder foler) {
        return null;
    }

    public List<Agent> findAgents(Folder folder) {
        return null;
    }

    public List<AgentDeployment> findAgentDeployments(Agent agent) {
        return null;
    }

}
