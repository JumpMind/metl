package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.AbstractObject;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeLinkData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public interface IConfigurationService {

    public List<Folder> findFolders(FolderType type);

    public void deleteFolder(String folderId);

    public void deleteComponentFlow(ComponentFlow flow);
    
    public void deleteComponentFlowNode(ComponentFlowNode flowNode);
    
    public void deleteComponentFlowLink(ComponentFlowNodeLinkData link);
    
    public List<ComponentFlow> findComponentFlowsInFolder(Folder folder);
    
    public void deleteComponentFlowVersion(ComponentFlowVersion componentFlowVersion);

    public void save(ComponentFlowVersion componentFlowVersion);
    
    public void refresh(ComponentFlowVersion componentFlowVersion);

    public abstract void save(AbstractObject<?> obj);

}
