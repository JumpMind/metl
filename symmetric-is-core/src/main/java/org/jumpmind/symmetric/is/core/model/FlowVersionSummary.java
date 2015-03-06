package org.jumpmind.symmetric.is.core.model;

import java.io.Serializable;

public class FlowVersionSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    String id;

    String name;

    String versionName;

    String folderName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

}
