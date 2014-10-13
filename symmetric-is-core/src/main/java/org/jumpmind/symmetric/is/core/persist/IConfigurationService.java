package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public interface IConfigurationService {

    public List<Folder> findFolders(FolderType type);

    public void deleteFolder(String folderId);

    public void deleteComponentFlow(ComponentFlow flow);
    
    public List<ComponentFlow> findComponentFlowsInFolder(Folder folder);
    
    public void deleteComponentFlowVersion(ComponentFlowVersion flowVersion);

    public void save(Folder folder);
    
    public void save(ComponentFlow flow);
    
    public void save(ComponentFlowVersion flowVersion);

}
