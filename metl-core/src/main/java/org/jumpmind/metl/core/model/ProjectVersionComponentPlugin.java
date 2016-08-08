package org.jumpmind.metl.core.model;

import java.io.Serializable;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

public class ProjectVersionComponentPlugin extends Plugin implements Serializable {

    private static final long serialVersionUID = 1L;
    
    static final GenericVersionScheme versionScheme = new GenericVersionScheme(); 

    String projectVersionId;
    String componentTypeId;
    String latestArtifactVersion;
    boolean enabled = true;
    boolean pinVersion = false;

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
    
    public boolean isUpdateAvailable() {
        try {
            if (latestArtifactVersion != null && artifactVersion != null) {
                Version latest = versionScheme.parseVersion(latestArtifactVersion);
                Version current = versionScheme.parseVersion(artifactVersion);
                return current.compareTo(latest) < 0;
            } else {
                return false;
            }
        } catch (InvalidVersionSpecificationException e) {            
            return false;
        }
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
