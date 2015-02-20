package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class ComponentFlow extends AbstractObject {

    private static final long serialVersionUID = 1L;

    List<ComponentFlowVersion> componentFlowVersions;

    Folder folder;

    String name;
    

    public ComponentFlow() {
        componentFlowVersions = new ArrayList<ComponentFlowVersion>();
    }

    public ComponentFlow(Folder folder) {
        this();
        setFolder(folder);
    }
    
    public ComponentFlow(String id) {
        this();
        this.id = id;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public List<ComponentFlowVersion> getComponentFlowVersions() {
        return componentFlowVersions;
    }

    public ComponentFlowVersion getLatestComponentFlowVersion() {
        ComponentFlowVersion latest = null;
        for (ComponentFlowVersion componentFlowVersion : componentFlowVersions) {
            if (latest == null
                    || latest.getCreateTime().before(componentFlowVersion.getCreateTime())) {
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
        if (folderId != null) {
            this.folder = new Folder(folderId);
        } else {
            this.folder = null;
        }
    }

    public String getFolderId() {
        return folder != null ? folder.getId() : null;
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

}
