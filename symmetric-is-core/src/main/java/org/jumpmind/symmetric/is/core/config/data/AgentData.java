package org.jumpmind.symmetric.is.core.config.data;

import java.util.Date;

import org.jumpmind.symmetric.is.core.config.AgentStartMode;
import org.jumpmind.symmetric.is.core.config.AgentStatus;

public class AgentData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String name;

    String folderId;

    String host;

    String startMode = AgentStartMode.AUTO.name();

    String status = AgentStatus.STOPPED.name();

    Date lastStartTime;

    Date heartbeatTime;

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

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setStartMode(String startMode) {
        this.startMode = startMode;
    }

    public String getStartMode() {
        return startMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(Date lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public Date getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(Date heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }
}
