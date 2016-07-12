package org.jumpmind.metl.core.model;

public class PluginArtifactVersion extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    String pluginArtifactId;
    
    boolean enabled = true;
    
    public PluginArtifactVersion() {
    }
    
    public PluginArtifactVersion(String groupName, String artifactName, String version) {
        setId(String.format("%s:%s:%s", groupName, artifactName, version));
        setPluginArtifactId(String.format("%s:%s", groupName, artifactName));
        setName(version);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setPluginArtifactId(String pluginArtifactId) {
        this.pluginArtifactId = pluginArtifactId;
    }
    
    public String getPluginArtifactId() {
        return pluginArtifactId;
    }

}
