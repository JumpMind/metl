package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.List;

public class Flow extends AbstractObject {

    private static final long serialVersionUID = 1L;

    List<FlowVersion> flowVersions;

    Folder folder;

    String name;
    
    public Flow() {
        flowVersions = new ArrayList<FlowVersion>();
    }

    public Flow(Folder folder) {
        this();
        setFolder(folder);
    }
    
    public Flow(String id) {
        this();
        this.id = id;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public List<FlowVersion> getFlowVersions() {
        return flowVersions;
    }

    public FlowVersion getLatestFlowVersion() {
        FlowVersion latest = null;
        for (FlowVersion flowVersion : flowVersions) {
            if (latest == null
                    || latest.getCreateTime().before(flowVersion.getCreateTime())) {
                latest = flowVersion;
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
