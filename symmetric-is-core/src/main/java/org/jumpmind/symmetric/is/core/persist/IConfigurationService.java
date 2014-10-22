package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public interface IConfigurationService {

    public List<Folder> findFolders(FolderType type);

    public void deleteFolder(String folderId);

    public void deleteComponentFlow(ComponentFlow flow);
    
    public void deleteComponentFlowNode(ComponentFlowNode flowNode);
    
    public void deleteComponentFlowLink(ComponentFlowNodeLink link);
    
    public void deleteConnection(Connection connection);
    
    public List<ComponentFlow> findComponentFlowsInFolder(Folder folder);
    
    public List<Connection> findConnectionsInFolder(Folder folder);
    
    public void deleteComponentFlowVersion(ComponentFlowVersion componentFlowVersion);

    public void save(ComponentFlowVersion componentFlowVersion);
    
    public void refresh(ComponentFlowVersion componentFlowVersion);
    
    public void refresh(Connection connection);

    public abstract void save(AbstractObject<?> obj);

}
