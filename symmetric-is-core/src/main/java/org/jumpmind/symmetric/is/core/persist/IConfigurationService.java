package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.FolderType;

public interface IConfigurationService {

    public abstract List<Folder> findFolders(FolderType type);

    public abstract void deleteFolder(String folderId);

    public abstract void save(Folder folder);

}
