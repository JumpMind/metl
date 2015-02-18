package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class ComponentFlow extends AbstractObject {

    private static final long serialVersionUID = 1L;

    List<ComponentFlowVersion> componentFlowVersions;

    Folder folder;
    
    String name;
    
    String folderId;
    
    public ComponentFlow() {
        componentFlowVersions = new ArrayList<ComponentFlowVersion>();
    }

    public ComponentFlow(Folder folder) {
        this();
        setFolder(folder);
    }
    
    public void setFolder(Folder folder) {
        this.folder = folder;
        if (folder != null) {
            folderId = folder.getId();
        } else {
            folderId = null;
        }
    }
    
    public List<ComponentFlowVersion> getComponentFlowVersions() {
        return componentFlowVersions;
    }

    public ComponentFlowVersion getLatestComponentFlowVersion() {
        ComponentFlowVersion latest = null;
        for (ComponentFlowVersion componentFlowVersion : componentFlowVersions) {
            if (latest == null
                    || latest.getCreateTime()
                            .before(componentFlowVersion.getCreateTime())) {
                latest = componentFlowVersion;
            }
        }
        return latest;
    }

    public Folder getFolder() {
        return folder;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
    
    public String getFolderId() {
        return folderId;
    }    

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

}
