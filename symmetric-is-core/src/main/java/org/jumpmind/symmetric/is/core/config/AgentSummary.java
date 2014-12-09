package org.jumpmind.symmetric.is.core.config;

import java.io.Serializable;

public class AgentSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    String id;

    String name;

    String host;

    String folderName;

    public AgentSummary() {
    }

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

    public String getHost() {
        return host;
    }

    public void setHost(String hostName) {
        this.host = hostName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }

}
