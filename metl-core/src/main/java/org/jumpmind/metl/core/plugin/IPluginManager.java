package org.jumpmind.metl.core.plugin;

import java.io.File;
import java.util.List;

import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;

public interface IPluginManager {
    
    public void init();
    
    public void refresh();
    
    public String toPluginId(String artifactGroup, String artifactName, String artifactVersion);
    
    public List<String> getAvailableVersions(String artifactGroup, String artifactName, List<PluginRepository> remoteRepositories);
    
    public String getLatestLocalVersion(String artifactGroup, String artifactName);
    
    public ClassLoader getClassLoader(String artifactGroup, String artifactName, String artifactVersion, List<PluginRepository> remoteRepositories);

    List<Plugin> getOutOfTheBox();

    boolean isNewer(Plugin first, Plugin second);

    void delete(String artifactGroup, String artifactName, String artifactVersion);
    
    public void install(String artifactGroup, String artifactName, String artifactVersion, File file);

}
