package org.jumpmind.metl.core.model;

import java.io.Serializable;
import java.util.Date;

public class ProjectVersionComponentPlugin extends Plugin implements Serializable {

    private static final long serialVersionUID = 1L;

    String projectVersionId;
    String componentTypeId;
    String latestArtifactVersion;
    boolean enabled = true;
    boolean pinVersion = false;
    Date createTime = new Date();
    String createBy;
    Date lastUpdateTime = new Date();
    String lastUpdateBy;

    public ProjectVersionComponentPlugin() {
    }

    public String getProjectVersionId() {
        return projectVersionId;
    }

    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
    }

    public String getComponentTypeId() {
        return componentTypeId;
    }

    public void setComponentTypeId(String componentType) {
        this.componentTypeId = componentType;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactGroup() {
        return artifactGroup;
    }

    public void setArtifactGroup(String artifactGroup) {
        this.artifactGroup = artifactGroup;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getLatestArtifactVersion() {
        return latestArtifactVersion;
    }

    public void setLatestArtifactVersion(String latestArtifactVersion) {
        this.latestArtifactVersion = latestArtifactVersion;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getLastUpdateBy() {
        return lastUpdateBy;
    }

    public void setLastUpdateBy(String lastUpdateBy) {
        this.lastUpdateBy = lastUpdateBy;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public void setPinVersion(boolean pinVersion) {
        this.pinVersion = pinVersion;
    }
    
    public boolean isPinVersion() {
        return pinVersion;
    }

}
