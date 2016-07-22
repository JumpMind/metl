package org.jumpmind.metl.core.model;

import java.io.Serializable;

public class Plugin implements Serializable, Comparable<Plugin> {

    private static final long serialVersionUID = 1L;

    String artifactName;
    String artifactGroup;
    String artifactVersion;

    public Plugin() {
    }
    
    public Plugin(String artifactGroup, String artifactName, String artifactVersion) {
        super();
        this.artifactName = artifactName;
        this.artifactGroup = artifactGroup;
        this.artifactVersion = artifactVersion;
    }
    
    public boolean matches(Plugin plugin) {
        return matches(plugin.getArtifactGroup(), plugin.getArtifactName());
    }
    
    public boolean matches(String artifactGroup, String artifactName) {
        boolean matches = false;
        if (this.artifactGroup != null && this.artifactGroup.equals(artifactGroup)) {
            if (this.artifactName != null && this.artifactName.equals(artifactName)) {
                matches = true;
            }
        }
        return matches;
    }

    public void setArtifactGroup(String artifactGroup) {
        this.artifactGroup = artifactGroup;
    }

    public String getArtifactGroup() {
        return artifactGroup;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }
    
    @Override
    public int compareTo(Plugin o) {
        int value = 0;
        if (o != null) {
            value = this.artifactGroup.compareTo(o.artifactGroup);
            if (value == 0) {
                value = this.artifactName.compareTo(o.artifactName);
                if (value == 0 && this.artifactVersion != null) {
                    value = this.artifactVersion.compareTo(o.artifactVersion);
                }
            }
        } else {
            value = 1;    
        }
        return value;        
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactGroup == null) ? 0 : artifactGroup.hashCode());
        result = prime * result + ((artifactName == null) ? 0 : artifactName.hashCode());
        result = prime * result + ((artifactVersion == null) ? 0 : artifactVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Plugin other = (Plugin) obj;
        if (artifactGroup == null) {
            if (other.artifactGroup != null)
                return false;
        } else if (!artifactGroup.equals(other.artifactGroup))
            return false;
        if (artifactName == null) {
            if (other.artifactName != null)
                return false;
        } else if (!artifactName.equals(other.artifactName))
            return false;
        if (artifactVersion == null) {
            if (other.artifactVersion != null)
                return false;
        } else if (!artifactVersion.equals(other.artifactVersion))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s:%s", artifactGroup, artifactName, artifactVersion);
    }
    
    

}
