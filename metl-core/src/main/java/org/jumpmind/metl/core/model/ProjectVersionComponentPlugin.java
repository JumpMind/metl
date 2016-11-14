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
    String componentName;
    String latestArtifactVersion;
    boolean enabled = true;
    boolean pinVersion = false;

    public ProjectVersionComponentPlugin() {
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public void setComponentName(String componentName) {
        this.componentName = componentName;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((componentName == null) ? 0 : componentName.hashCode());
        result = prime * result + ((componentTypeId == null) ? 0 : componentTypeId.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((latestArtifactVersion == null) ? 0 : latestArtifactVersion.hashCode());
        result = prime * result + (pinVersion ? 1231 : 1237);
        result = prime * result + ((projectVersionId == null) ? 0 : projectVersionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProjectVersionComponentPlugin other = (ProjectVersionComponentPlugin) obj;
        if (componentName == null) {
            if (other.componentName != null)
                return false;
        } else if (!componentName.equals(other.componentName))
            return false;
        if (componentTypeId == null) {
            if (other.componentTypeId != null)
                return false;
        } else if (!componentTypeId.equals(other.componentTypeId))
            return false;
        if (enabled != other.enabled)
            return false;
        if (latestArtifactVersion == null) {
            if (other.latestArtifactVersion != null)
                return false;
        } else if (!latestArtifactVersion.equals(other.latestArtifactVersion))
            return false;
        if (pinVersion != other.pinVersion)
            return false;
        if (projectVersionId == null) {
            if (other.projectVersionId != null)
                return false;
        } else if (!projectVersionId.equals(other.projectVersionId))
            return false;
        return true;
    }
    
    

}
