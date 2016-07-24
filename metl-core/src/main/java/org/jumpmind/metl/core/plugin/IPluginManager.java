package org.jumpmind.metl.core.plugin;

public interface IPluginManager {
    
    public void init();
    
    public void refresh();
    
    public String toPluginId(String artifactGroup, String artifactName, String artifactVersion);
    
    public String getLatestLocalVersion(String artifactGroup, String artifactName);
    
    public ClassLoader getClassLoader(String artifactGroup, String artifactName, String artifactVersion);

}
