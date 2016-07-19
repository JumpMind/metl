package org.jumpmind.metl.core.plugin;

public interface IPluginManager {
    
    public void init();
    
    public String getLatestLocalVersion(String artifactGroup, String artifactName);
    
    public ClassLoader getClassLoader(String artifactGroup, String artifactName, String artifactVersion);

    public void reload();
}
