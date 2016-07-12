package org.jumpmind.metl.core.model;

public class PluginArtifact extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    String groupName;
    
    String pluginType;
    
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

}
