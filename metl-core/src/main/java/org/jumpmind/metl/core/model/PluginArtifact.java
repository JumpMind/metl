package org.jumpmind.metl.core.model;

import java.util.ArrayList;
import java.util.List;

public class PluginArtifact extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    String groupName;
    
    String pluginType;
    
    List<PluginArtifactVersion> pluginArtifactVersions;
    
    public PluginArtifact(String artifactName, String groupName, PluginType type, String version) {
        this(artifactName, groupName, type);
        addPluginArtifactVersion(new PluginArtifactVersion(groupName, artifactName, version));
    }
    
    public PluginArtifact(String name, String group, PluginType type) {
        this(name, group, type.name());
    }

    public PluginArtifact(String name, String group, String type) {
        this.setId(String.format("%s:%s", group, name));
        this.name = name;
        this.groupName = group;
        this.pluginType = type;
    }
    
    public PluginArtifact() {
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    public void setGroupName(String group) {
        this.groupName = group;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public String getPluginType() {
        return pluginType;
    }
    
    public void setPluginType(String type) {
        this.pluginType = type;
    }
    
    public void addPluginArtifactVersion(PluginArtifactVersion version) {
        if (pluginArtifactVersions == null) {
            pluginArtifactVersions = new ArrayList<>();
        }
        pluginArtifactVersions.remove(version);
        pluginArtifactVersions.add(version);
    }
    
    public void setPluginArtifactVersions(List<PluginArtifactVersion> pluginArtifactVersions) {
        this.pluginArtifactVersions = pluginArtifactVersions;
    }
    
    public List<PluginArtifactVersion> getPluginArtifactVersions() {
        return pluginArtifactVersions;
    }

}
